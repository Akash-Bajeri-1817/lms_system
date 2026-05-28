package com.lms.notification.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    // every email shares this wrapper
    private String wrap(String content) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px;
                         margin: 0 auto; padding: 20px; color: #333;">
                <div style="background: #4F46E5; padding: 20px;
                             border-radius: 8px 8px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">LMS Platform</h1>
                </div>
                <div style="background: #f9f9f9; padding: 30px;
                             border-radius: 0 0 8px 8px; border: 1px solid #eee;">
                    %s
                </div>
                <p style="text-align: center; color: #999; font-size: 12px;
                           margin-top: 20px;">
                    © 2026 LMS Platform. All rights reserved.
                </p>
            </body>
            </html>
            """.formatted(content);
    }

    public String welcomeEmail(String firstName) {
        return wrap("""
            <h2>Welcome to LMS Platform, %s! 🎉</h2>
            <p>We're excited to have you on board.</p>
            <p>You can now:</p>
            <ul>
                <li>Browse hundreds of courses</li>
                <li>Learn at your own pace</li>
                <li>Earn certificates on completion</li>
            </ul>
            <a href="http://localhost:8080/api/courses/published"
               style="background: #4F46E5; color: white; padding: 12px 24px;
                      border-radius: 6px; text-decoration: none;
                      display: inline-block; margin-top: 16px;">
                Browse Courses
            </a>
            """.formatted(firstName));
    }

    public String enrollmentEmail(String firstName, String courseTitle,
                                  String instructorName) {
        return wrap("""
            <h2>You're enrolled! 📚</h2>
            <p>Hi %s,</p>
            <p>You have successfully enrolled in:</p>
            <div style="background: white; padding: 16px; border-radius: 8px;
                        border-left: 4px solid #4F46E5; margin: 16px 0;">
                <h3 style="margin: 0; color: #4F46E5;">%s</h3>
                <p style="margin: 4px 0; color: #666;">
                    Instructor: %s
                </p>
            </div>
            <p>Start learning today and earn your certificate!</p>
            """.formatted(firstName, courseTitle, instructorName));
    }

    public String paymentSuccessEmail(String firstName, String courseTitle,
                                      String amount, String orderId) {
        return wrap("""
            <h2>Payment Successful! ✅</h2>
            <p>Hi %s,</p>
            <p>Your payment has been processed successfully.</p>
            <div style="background: white; padding: 16px; border-radius: 8px;
                        border: 1px solid #eee; margin: 16px 0;">
                <table style="width: 100%%;">
                    <tr>
                        <td style="color: #666;">Course</td>
                        <td style="font-weight: bold;">%s</td>
                    </tr>
                    <tr>
                        <td style="color: #666;">Amount Paid</td>
                        <td style="font-weight: bold; color: #16a34a;">
                            ₹%s
                        </td>
                    </tr>
                    <tr>
                        <td style="color: #666;">Order ID</td>
                        <td style="font-size: 12px; color: #999;">%s</td>
                    </tr>
                </table>
            </div>
            <p>You are now enrolled and can start learning immediately!</p>
            """.formatted(firstName, courseTitle, amount, orderId));
    }

    public String certificateEmail(String firstName, String courseTitle,
                                   String certificateNumber) {
        return wrap("""
            <h2>Congratulations! You've earned a certificate! 🎓</h2>
            <p>Hi %s,</p>
            <p>You have successfully completed:</p>
            <div style="background: white; padding: 16px; border-radius: 8px;
                        border-left: 4px solid #16a34a; margin: 16px 0;">
                <h3 style="margin: 0; color: #16a34a;">%s</h3>
                <p style="margin: 8px 0; color: #666;">
                    Certificate Number:
                    <strong>%s</strong>
                </p>
            </div>
            <p>Share your achievement with the world!</p>
            <a href="http://localhost:8080/api/progress/certificates/verify/%s"
               style="background: #16a34a; color: white; padding: 12px 24px;
                      border-radius: 6px; text-decoration: none;
                      display: inline-block; margin-top: 16px;">
                Verify Certificate
            </a>
            """.formatted(firstName, courseTitle,
                certificateNumber, certificateNumber));
    }

    public String quizResultEmail(String firstName, String quizTitle,
                                  int score, int totalMarks,
                                  String percentage, boolean passed) {
        String statusColor = passed ? "#16a34a" : "#dc2626";
        String statusText = passed ? "PASSED ✅" : "FAILED ❌";

        return wrap("""
            <h2>Quiz Result 📝</h2>
            <p>Hi %s,</p>
            <p>Your quiz has been graded:</p>
            <div style="background: white; padding: 16px; border-radius: 8px;
                        border: 1px solid #eee; margin: 16px 0;">
                <h3 style="margin: 0 0 12px;">%s</h3>
                <table style="width: 100%%;">
                    <tr>
                        <td style="color: #666;">Score</td>
                        <td><strong>%d / %d</strong></td>
                    </tr>
                    <tr>
                        <td style="color: #666;">Percentage</td>
                        <td><strong>%s%%</strong></td>
                    </tr>
                    <tr>
                        <td style="color: #666;">Result</td>
                        <td style="color: %s; font-weight: bold;">%s</td>
                    </tr>
                </table>
            </div>
            """.formatted(firstName, quizTitle, score,
                totalMarks, percentage, statusColor, statusText));
    }

    public String instructorApprovedEmail(String firstName) {
        return wrap("""
            <h2>Congratulations! You're now an Instructor! 🎉</h2>
            <p>Hi %s,</p>
            <p>Your instructor application has been
               <strong>approved</strong>!</p>
            <p>You can now:</p>
            <ul>
                <li>Create and publish courses</li>
                <li>Add modules and lessons</li>
                <li>Create quizzes and assignments</li>
                <li>View student progress and results</li>
            </ul>
            <p>Login again to get your updated instructor token and
               start creating!</p>
            """.formatted(firstName));
    }

    public String instructorRejectedEmail(String firstName,
                                          String reason) {
        return wrap("""
            <h2>Instructor Application Update</h2>
            <p>Hi %s,</p>
            <p>Unfortunately, your instructor application was not approved
               at this time.</p>
            <div style="background: white; padding: 16px; border-radius: 8px;
                        border-left: 4px solid #dc2626; margin: 16px 0;">
                <p style="margin: 0; color: #666;">
                    <strong>Reason:</strong> %s
                </p>
            </div>
            <p>You're welcome to apply again after addressing the
               feedback above.</p>
            """.formatted(firstName, reason));
    }
}