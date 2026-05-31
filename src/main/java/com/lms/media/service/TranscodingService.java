package com.lms.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodingService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // resolution definitions
    private static final int[][] RESOLUTIONS = {
            {1280, 720,  2000},   // 720p  — width, height, bitrate(kbps)
            {854,  480,  1000},   // 480p
            {640,  360,  600}     // 360p
    };

    private static final String[] RESOLUTION_NAMES = {
            "720p", "480p", "360p"
    };

    public List<String> transcodeVideo(Long lessonId,
                                       String rawVideoKey) {
        Path tempDir = null;
        List<String> transcodedKeys = new ArrayList<>();

        try {
            // create temp directory for processing
            tempDir = Files.createTempDirectory("lms-transcode-");
            log.info("Transcoding video for lesson: {} in {}",
                    lessonId, tempDir);

            // download raw video from S3
            Path rawVideoPath = tempDir.resolve("raw_video.mp4");
            downloadFromS3(rawVideoKey, rawVideoPath);
            log.info("Raw video downloaded: {} bytes",
                    Files.size(rawVideoPath));

            // transcode to each resolution
            List<String> m3u8Keys = new ArrayList<>();

            for (int i = 0; i < RESOLUTIONS.length; i++) {
                int width    = RESOLUTIONS[i][0];
                int height   = RESOLUTIONS[i][1];
                int bitrate  = RESOLUTIONS[i][2];
                String name  = RESOLUTION_NAMES[i];

                log.info("Transcoding to {}...", name);

                // output directory for this resolution
                Path outputDir = tempDir.resolve(name);
                Files.createDirectories(outputDir);

                Path m3u8Path = outputDir.resolve("index.m3u8");

                // run FFmpeg
                boolean success = runFFmpeg(
                        rawVideoPath, m3u8Path,
                        width, height, bitrate
                );

                if (success) {
                    // upload all HLS segments to S3
                    String m3u8Key = uploadHlsSegments(
                            lessonId, name, outputDir
                    );
                    m3u8Keys.add(name + ":" + m3u8Key);
                    transcodedKeys.add(m3u8Key);
                    log.info("Transcoded and uploaded: {}", name);
                } else {
                    log.error("FFmpeg failed for resolution: {}", name);
                }
            }

            // generate master playlist
            String masterKey = generateMasterPlaylist(
                    lessonId, m3u8Keys, tempDir
            );
            transcodedKeys.add(0, masterKey);  // master goes first

            log.info("Transcoding complete for lesson: {}. " +
                    "Master playlist: {}", lessonId, masterKey);

            return transcodedKeys;

        } catch (Exception e) {
            log.error("Transcoding failed for lesson {}: {}",
                    lessonId, e.getMessage());
            throw new RuntimeException(
                    "Transcoding failed: " + e.getMessage()
            );
        } finally {
            // always clean up temp files
            if (tempDir != null) {
                cleanupTempDir(tempDir);
            }
        }
    }

    // ── FFMPEG ───────────────────────────────────────────────────────

    private boolean runFFmpeg(Path inputPath, Path outputM3u8,
                              int width, int height,
                              int bitrate) throws IOException,
            InterruptedException {
        // FFmpeg command to convert to HLS format
        List<String> command = List.of(
                "ffmpeg",
                "-i", inputPath.toString(),         // input file
                "-vf", "scale=" + width + ":" + height,  // resolution
                "-c:v", "libx264",                  // video codec
                "-b:v", bitrate + "k",              // video bitrate
                "-c:a", "aac",                      // audio codec
                "-b:a", "128k",                     // audio bitrate
                "-hls_time", "10",                  // 10 second segments
                "-hls_list_size", "0",              // keep all segments
                "-hls_segment_filename",
                outputM3u8.getParent()
                        .resolve("segment_%03d.ts").toString(),
                outputM3u8.toString(),              // output .m3u8 file
                "-y"                                // overwrite if exists
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // log FFmpeg output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    // ── S3 OPERATIONS ────────────────────────────────────────────────

    private void downloadFromS3(String key,
                                Path destination) throws IOException {
        s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                destination
        );
    }

    private String uploadHlsSegments(Long lessonId, String resolution,
                                     Path outputDir) throws IOException {
        String m3u8Key = null;

        // upload all files in the resolution directory
        File[] files = outputDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                String key = "videos/hls/lesson-" + lessonId
                        + "/" + resolution + "/" + file.getName();

                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(key)
                                .contentType(getContentType(file.getName()))
                                .build(),
                        RequestBody.fromFile(file)
                );

                if (file.getName().equals("index.m3u8")) {
                    m3u8Key = key;
                }
            }
        }
        return m3u8Key;
    }

    private String generateMasterPlaylist(Long lessonId,
                                          List<String> m3u8Keys,
                                          Path tempDir) throws IOException {
        // HLS master playlist tells the player about available qualities
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n");
        master.append("#EXT-X-VERSION:3\n\n");

        int[][] bandwidths = {
                {2000000},  // 720p — 2 Mbps
                {1000000},  // 480p — 1 Mbps
                {600000}    // 360p — 600 Kbps
        };

        String[] names = {"720p", "480p", "360p"};
        String[] resolutions = {"1280x720", "854x480", "640x360"};

        for (int i = 0; i < m3u8Keys.size(); i++) {
            String entry = m3u8Keys.get(i);
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String name = parts[0];
                String key  = parts[1];

                // find index for this resolution
                int idx = List.of(names).indexOf(name);
                if (idx >= 0) {
                    master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                            .append(bandwidths[idx][0])
                            .append(",RESOLUTION=")
                            .append(resolutions[idx])
                            .append(",NAME=\"").append(name).append("\"\n")
                            .append(key).append("\n\n");
                }
            }
        }

        // save master playlist to temp file
        Path masterPath = tempDir.resolve("master.m3u8");
        Files.writeString(masterPath, master.toString());

        // upload master playlist to S3
        String masterKey = "videos/hls/lesson-" + lessonId
                + "/master.m3u8";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(masterKey)
                        .contentType("application/vnd.apple.mpegurl")
                        .build(),
                RequestBody.fromFile(masterPath.toFile())
        );

        return masterKey;
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private String getContentType(String filename) {
        if (filename.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        } else if (filename.endsWith(".ts")) {
            return "video/MP2T";
        }
        return "application/octet-stream";
    }

    private void cleanupTempDir(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            log.info("Cleaned up temp directory: {}", tempDir);
        } catch (IOException e) {
            log.warn("Failed to cleanup temp directory: {}",
                    e.getMessage());
        }
    }
}