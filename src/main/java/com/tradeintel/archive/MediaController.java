package com.tradeintel.archive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves locally-stored WhatsApp media files.
 * Files are stored at {@code <storage-dir>/<groupId>/<filename>}.
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final Logger log = LogManager.getLogger(MediaController.class);

    private final Path storageDir;

    public MediaController(@Value("${app.media.storage-dir:./media}") String storageDir) {
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @GetMapping("/{groupId}/{filename}")
    public ResponseEntity<Resource> serveMedia(
            @PathVariable String groupId,
            @PathVariable String filename) {

        Path filePath = storageDir.resolve(groupId).resolve(filename).toAbsolutePath().normalize();

        // Prevent path traversal
        if (!filePath.startsWith(storageDir)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        String contentType = resolveContentType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }

    private String resolveContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
