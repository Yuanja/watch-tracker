package com.tradeintel.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WhapiMessageDTO {

    @JsonProperty("messages")
    private List<Message> messages;

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {

        @JsonProperty("id")
        private String id;

        @JsonProperty("from")
        private String from;

        @JsonProperty("chat_id")
        private String chatId;

        @JsonProperty("from_name")
        private String fromName;

        @JsonProperty("text")
        private MessageText text;

        @JsonProperty("image")
        private MediaContent image;

        @JsonProperty("document")
        private MediaContent document;

        @JsonProperty("video")
        private MediaContent video;

        @JsonProperty("audio")
        private MediaContent audio;

        @JsonProperty("timestamp")
        private Long timestamp;

        @JsonProperty("from_me")
        private boolean fromMe;

        @JsonProperty("forwarded")
        private boolean forwarded;

        @JsonProperty("quoted_msg")
        private QuotedMessage quotedMsg;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        public MessageText getText() { return text; }
        public void setText(MessageText text) { this.text = text; }
        public MediaContent getImage() { return image; }
        public void setImage(MediaContent image) { this.image = image; }
        public MediaContent getDocument() { return document; }
        public void setDocument(MediaContent document) { this.document = document; }
        public MediaContent getVideo() { return video; }
        public void setVideo(MediaContent video) { this.video = video; }
        public MediaContent getAudio() { return audio; }
        public void setAudio(MediaContent audio) { this.audio = audio; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        public boolean isFromMe() { return fromMe; }
        public void setFromMe(boolean fromMe) { this.fromMe = fromMe; }
        public boolean isForwarded() { return forwarded; }
        public void setForwarded(boolean forwarded) { this.forwarded = forwarded; }
        public QuotedMessage getQuotedMsg() { return quotedMsg; }
        public void setQuotedMsg(QuotedMessage quotedMsg) { this.quotedMsg = quotedMsg; }

        public String getMessageBody() {
            if (text != null && text.getBody() != null) {
                return text.getBody();
            }
            if (image != null && image.getCaption() != null) {
                return image.getCaption();
            }
            return null;
        }

        public String getMessageType() {
            if (image != null) return "image";
            if (document != null) return "document";
            if (video != null) return "video";
            if (audio != null) return "audio";
            return "text";
        }

        public String getMediaUrl() {
            if (image != null) return image.getLink();
            if (document != null) return document.getLink();
            if (video != null) return video.getLink();
            if (audio != null) return audio.getLink();
            return null;
        }

        public String getMediaMimeType() {
            if (image != null) return image.getMimeType();
            if (document != null) return document.getMimeType();
            if (video != null) return video.getMimeType();
            if (audio != null) return audio.getMimeType();
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageText {
        @JsonProperty("body")
        private String body;

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MediaContent {
        @JsonProperty("link")
        private String link;

        @JsonProperty("caption")
        private String caption;

        @JsonProperty("mime_type")
        private String mimeType;

        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public String getCaption() { return caption; }
        public void setCaption(String caption) { this.caption = caption; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuotedMessage {
        @JsonProperty("id")
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
