package com.lms.media.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {
    private Long lessonId;
    private String rawVideoKey;     // S3 key of uploaded raw video
    private String originalFileName;
}