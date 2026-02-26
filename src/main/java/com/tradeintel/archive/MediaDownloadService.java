package com.tradeintel.archive;

import com.tradeintel.common.entity.RawMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Downloads WhatsApp media files (images, documents, videos, audio) to local
 * filesystem storage. In production this would be swapped with an S3 client.
 *
 * <p>The service is called after a message is archived. If the message has a
 * {@code mediaUrl}, the file is downloaded and stored under
 * {@code ${app.media.storage-dir}/<groupId>/<messageId>.<ext>}.</p>
 */
@Service
public class MediaDownloadService {

    private static final Logger log = LogManager.getLogger(MediaDownloadService.class);

    private final RestTemplate restTemplate;
    private final Path storageDir;

    public MediaDownloadService(
            @Value("${app.media.storage-dir:./media}") String storageDir) {
        this.restTemplate = new RestTemplate();
        this.storageDir = Path.of(storageDir);
    }

    /**
     * Downloads the media file for a raw message and updates its
     * {@code mediaLocalPath} field.
     *
     * @param message the archived raw message with a non-null mediaUrl
     * @return the local file path, or null if download failed or no media present
     */
    public String download(RawMessage message) {
        if (message.getMediaUrl() == null || message.getMediaUrl().isBlank()) {
            return null;
        }

        try {
            String groupId = message.getGroup() != null
                    ? message.getGroup().getId().toString()
                    : "unknown";

            Path groupDir = storageDir.resolve(groupId);
            Files.createDirectories(groupDir);

            String extension = resolveExtension(message.getMediaMimeType());
            String fileName = message.getId().toString() + extension;
            Path filePath = groupDir.resolve(fileName);

            byte[] data = restTemplate.getForObject(message.getMediaUrl(), byte[].class);
            if (data == null || data.length == 0) {
                log.warn("Empty response downloading media for message={}", message.getWhapiMsgId());
                return null;
            }

            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String localPath = filePath.toString();
            log.info("Downloaded media for message={}: {} ({} bytes)",
                    message.getWhapiMsgId(), localPath, data.length);

            return localPath;

        } catch (IOException e) {
            log.error("Failed to save media for message={}: {}",
                    message.getWhapiMsgId(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to download media for message={}: {}",
                    message.getWhapiMsgId(), e.getMessage());
            return null;
        }
    }

    private String resolveExtension(String mimeType) {
        if (mimeType == null) return "";
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "video/mp4" -> ".mp4";
            case "audio/ogg" -> ".ogg";
            case "audio/mpeg" -> ".mp3";
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            default -> "";
        };
    }
}
