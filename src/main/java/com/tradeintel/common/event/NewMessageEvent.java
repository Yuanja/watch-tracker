package com.tradeintel.common.event;

import java.util.UUID;

public class NewMessageEvent {

    private final UUID messageId;

    public NewMessageEvent(UUID messageId) {
        this.messageId = messageId;
    }

    public UUID getMessageId() {
        return messageId;
    }
}
