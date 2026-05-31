package com.lms.media.consumer;

import com.lms.course.repository.LessonRepository;
import com.lms.media.event.VideoUploadedEvent;
import com.lms.media.service.TranscodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTranscodingConsumer {

    private final TranscodingService transcodingService;
    private final LessonRepository lessonRepository;

    @KafkaListener(
            topics = "video.uploaded",
            groupId = "lms-transcoding-group"
    )
    public void handleVideoUploaded(VideoUploadedEvent event) {
        log.info("Received video.uploaded event for lesson: {}",
                event.getLessonId());

        try {
            // transcode the video
            List<String> transcodedKeys = transcodingService
                    .transcodeVideo(
                            event.getLessonId(),
                            event.getRawVideoKey()
                    );

            // update lesson with master playlist key
            // first key is always the master playlist
            String masterPlaylistKey = transcodedKeys.get(0);

            lessonRepository.findById(event.getLessonId())
                    .ifPresent(lesson -> {
                        lesson.setContentUrl(masterPlaylistKey);
                        lessonRepository.save(lesson);
                        log.info("Lesson {} updated with HLS playlist: {}",
                                event.getLessonId(), masterPlaylistKey);
                    });

        } catch (Exception e) {
            log.error("Transcoding failed for lesson {}: {}",
                    event.getLessonId(), e.getMessage());
            // in production — publish to a dead letter topic
            // so failed jobs can be retried
        }
    }
}