package com.lms.media.producer;

import com.lms.media.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEventProducer {

    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    private static final String TOPIC = "video.uploaded";

    public void publishVideoUploaded(Long lessonId, String rawVideoKey,
                                     String originalFileName) {
        VideoUploadedEvent event = VideoUploadedEvent.builder()
                .lessonId(lessonId)
                .rawVideoKey(rawVideoKey)
                .originalFileName(originalFileName)
                .build();

        kafkaTemplate.send(TOPIC, String.valueOf(lessonId), event);
        log.info("Published video.uploaded event for lesson: {}", lessonId);
    }
}