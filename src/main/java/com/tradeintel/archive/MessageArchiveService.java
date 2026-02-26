package com.tradeintel.archive;

import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.webhook.WhapiMessageDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class MessageArchiveService {

    private static final Logger log = LogManager.getLogger(MessageArchiveService.class);

    private final RawMessageRepository rawMessageRepository;
    private final WhatsappGroupRepository groupRepository;
    private final MediaDownloadService mediaDownloadService;

    public MessageArchiveService(RawMessageRepository rawMessageRepository,
                                 WhatsappGroupRepository groupRepository,
                                 MediaDownloadService mediaDownloadService) {
        this.rawMessageRepository = rawMessageRepository;
        this.groupRepository = groupRepository;
        this.mediaDownloadService = mediaDownloadService;
    }

    @Transactional
    public RawMessage archive(WhapiMessageDTO.Message msg) {
        if (msg.getId() == null || msg.getChatId() == null) {
            log.warn("Skipping message with null id or chatId");
            return null;
        }

        // Idempotent: skip if already archived
        if (rawMessageRepository.existsByWhapiMsgId(msg.getId())) {
            log.debug("Message already archived: {}", msg.getId());
            return null;
        }

        // Find or create group
        WhatsappGroup group = groupRepository.findByWhapiGroupId(msg.getChatId())
                .orElseGet(() -> {
                    WhatsappGroup newGroup = new WhatsappGroup();
                    newGroup.setWhapiGroupId(msg.getChatId());
                    newGroup.setGroupName(msg.getChatId()); // Will be updated later
                    newGroup.setIsActive(true);
                    return groupRepository.save(newGroup);
                });

        RawMessage rawMessage = new RawMessage();
        rawMessage.setGroup(group);
        rawMessage.setWhapiMsgId(msg.getId());
        rawMessage.setSenderPhone(msg.getFrom());
        rawMessage.setSenderName(msg.getFromName());
        rawMessage.setMessageBody(msg.getMessageBody());
        rawMessage.setMessageType(msg.getMessageType());
        rawMessage.setMediaUrl(msg.getMediaUrl());
        rawMessage.setMediaMimeType(msg.getMediaMimeType());
        rawMessage.setIsForwarded(msg.isForwarded());
        rawMessage.setProcessed(false);

        if (msg.getQuotedMsg() != null) {
            rawMessage.setReplyToMsgId(msg.getQuotedMsg().getId());
        }

        if (msg.getTimestamp() != null) {
            rawMessage.setTimestampWa(
                    OffsetDateTime.ofInstant(Instant.ofEpochSecond(msg.getTimestamp()), ZoneOffset.UTC));
        } else {
            rawMessage.setTimestampWa(OffsetDateTime.now(ZoneOffset.UTC));
        }

        try {
            RawMessage saved = rawMessageRepository.save(rawMessage);

            // Download media to local storage if present
            if (saved.getMediaUrl() != null && !saved.getMediaUrl().isBlank()) {
                String localPath = mediaDownloadService.download(saved);
                if (localPath != null) {
                    saved.setMediaLocalPath(localPath);
                    rawMessageRepository.save(saved);
                }
            }

            return saved;
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate message ignored: {}", msg.getId());
            return null;
        }
    }
}
