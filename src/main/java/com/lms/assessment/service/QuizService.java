package com.lms.assessment.service;

import com.lms.assessment.dto.*;
import com.lms.assessment.entity.*;
import com.lms.assessment.repository.QuizRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.EmailTemplateService;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailTemplateService templateService;

    // INSTRUCTOR creates a quiz with questions and options in one request
    @Transactional
    public QuizResponse createQuiz(Long courseId, QuizRequest request) {

        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // calculate total marks from all questions
        int totalMarks = request.getQuestions().stream()
                .mapToInt(QuestionRequest::getMarks)
                .sum();

        var quiz = Quiz.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .passingScore(request.getPassingScore())
                .timeLimit(request.getTimeLimit())
                .totalMarks(totalMarks)
                .published(false)
                .build();

        // build questions and options
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < request.getQuestions().size(); i++) {
            QuestionRequest qReq = request.getQuestions().get(i);

            // validate — each question must have exactly one correct answer
            long correctCount = qReq.getOptions().stream()
                    .filter(OptionRequest::isCorrect).count();
            if (correctCount != 1) {
                throw new RuntimeException(
                        "Question " + (i + 1) + " must have exactly one correct answer"
                );
            }

            Question question = Question.builder()
                    .quiz(quiz)
                    .questionText(qReq.getQuestionText())
                    .marks(qReq.getMarks())
                    .position(i)
                    .build();

            List<Option> options = qReq.getOptions().stream()
                    .map(oReq -> Option.builder()
                            .question(question)
                            .optionText(oReq.getOptionText())
                            .correct(oReq.isCorrect())
                            .build())
                    .collect(Collectors.toList());

            question.setOptions(options);
            questions.add(question);
        }

        quiz.setQuestions(questions);
        return mapToResponse(quizRepository.save(quiz), true);
    }

    // INSTRUCTOR publishes a quiz so students can take it
    public QuizResponse publishQuiz(Long quizId) {
        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        quiz.setPublished(true);
        return mapToResponse(quizRepository.save(quiz), false);
    }

    // STUDENT gets quiz to take — options shown WITHOUT correct answer
    public QuizResponse getQuizForStudent(Long quizId) {
        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!quiz.isPublished()) {
            throw new RuntimeException("Quiz is not available yet");
        }

        return mapToResponse(quiz, false);  // false = hide correct answers
    }

    // INSTRUCTOR sees quiz with correct answers
    public QuizResponse getQuizForInstructor(Long quizId) {
        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        return mapToResponse(quiz, true);   // true = show correct answers
    }

    // get all quizzes for a course
    public List<QuizResponse> getQuizzesByCourse(Long courseId) {
        return quizRepository.findByCourseId(courseId)
                .stream()
                .map(q -> mapToResponse(q, false))
                .toList();
    }

    // STUDENT submits quiz — auto-grading happens here
    @Transactional
    public SubmissionResponse submitQuiz(Long quizId, SubmitQuizRequest request) {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!quiz.isPublished()) {
            throw new RuntimeException("Quiz is not available");
        }

        // prevent re-submission
        if (submissionRepository.existsByStudentIdAndQuizId(
                student.getId(), quizId)) {
            throw new RuntimeException(
                    "You have already submitted this quiz"
            );
        }

        // ── AUTO GRADING LOGIC ──────────────────────────────────────
        Map<Long, Long> studentAnswers = request.getAnswers();
        int score = 0;
        int attempted = 0;
        List<SubmissionAnswer> submissionAnswers = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {
            Long selectedOptionId = studentAnswers.get(question.getId());

            if (selectedOptionId != null) {
                attempted++;

                // find the selected option
                Option selectedOption = question.getOptions().stream()
                        .filter(o -> o.getId().equals(selectedOptionId))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException("Invalid option selected")
                        );

                boolean isCorrect = selectedOption.isCorrect();
                if (isCorrect) {
                    score += question.getMarks();   // add marks if correct
                }

                submissionAnswers.add(SubmissionAnswer.builder()
                        .question(question)
                        .selectedOption(selectedOption)
                        .correct(isCorrect)
                        .build());
            } else {
                // skipped question
                submissionAnswers.add(SubmissionAnswer.builder()
                        .question(question)
                        .selectedOption(null)
                        .correct(false)
                        .build());
            }
        }

        // calculate percentage
        BigDecimal percentage = quiz.getTotalMarks() > 0
                ? BigDecimal.valueOf(score)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(quiz.getTotalMarks()), 2,
                        RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        boolean passed = percentage.compareTo(
                BigDecimal.valueOf(quiz.getPassingScore())) >= 0;
        // ── END GRADING ─────────────────────────────────────────────

        var submission = Submission.builder()
                .student(student)
                .quiz(quiz)
                .score(score)
                .totalMarks(quiz.getTotalMarks())
                .totalQuestions(quiz.getQuestions().size())
                .attempted(attempted)
                .percentage(percentage)
                .passed(passed)
                .build();

        // link answers to submission
        submissionAnswers.forEach(a -> a.setSubmission(submission));
        submission.setAnswers(submissionAnswers);

        var saved = submissionRepository.save(submission);
        emailService.sendEmail(
                student.getEmail(),
                "Quiz Result: " + quiz.getTitle(),
                templateService.quizResultEmail(
                        student.getFirstName(),
                        quiz.getTitle(),
                        saved.getScore(),
                        saved.getTotalMarks(),
                        saved.getPercentage().toString(),
                        saved.isPassed()
                )
        );
        return mapToSubmissionResponse(saved);
    }

    // STUDENT views their result
    public SubmissionResponse getMyResult(Long quizId) {
        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var submission = submissionRepository
                .findByStudentIdAndQuizId(student.getId(), quizId)
                .orElseThrow(() ->
                        new RuntimeException("No submission found for this quiz")
                );

        return mapToSubmissionResponse(submission);
    }

    // INSTRUCTOR views all results for a quiz
    public List<SubmissionResponse> getQuizResults(Long quizId) {
        return submissionRepository.findByQuizId(quizId)
                .stream()
                .map(this::mapToSubmissionResponse)
                .toList();
    }

    // ── MAPPERS ──────────────────────────────────────────────────────

    private QuizResponse mapToResponse(Quiz quiz, boolean showCorrectAnswers) {
        List<QuestionResponse> questionResponses = quiz.getQuestions()
                .stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .marks(q.getMarks())
                        .position(q.getPosition())
                        .options(q.getOptions().stream()
                                .map(o -> OptionResponse.builder()
                                        .id(o.getId())
                                        .optionText(o.getOptionText())
                                        // only show correct answer to instructor
                                        .correct(showCorrectAnswers
                                                ? o.isCorrect()
                                                : null)
                                        .build())
                                .toList())
                        .build())
                .toList();

        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .passingScore(quiz.getPassingScore())
                .timeLimit(quiz.getTimeLimit())
                .totalMarks(quiz.getTotalMarks())
                .published(quiz.isPublished())
                .questionCount(quiz.getQuestions().size())
                .createdAt(quiz.getCreatedAt())
                .questions(questionResponses)
                .build();
    }

    private SubmissionResponse mapToSubmissionResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .quizTitle(s.getQuiz().getTitle())
                .score(s.getScore())
                .totalMarks(s.getTotalMarks())
                .totalQuestions(s.getTotalQuestions())
                .attempted(s.getAttempted())
                .percentage(s.getPercentage())
                .passed(s.isPassed())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}