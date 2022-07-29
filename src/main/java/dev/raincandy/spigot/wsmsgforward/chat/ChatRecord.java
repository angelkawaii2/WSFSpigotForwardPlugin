package dev.raincandy.spigot.wsmsgforward.chat;

import java.util.UUID;

/**
 * 记录发出minecraft消息的玩家信息
 */
public class ChatRecord {
    String requestId;
    String originalMsg;
    UUID sender;

    public ChatRecord(String requestId, String originalMsg, UUID senderUuid) {
        this.requestId = requestId;
        this.originalMsg = originalMsg;
        this.sender = senderUuid;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getOriginalMsg() {
        return originalMsg;
    }

    public UUID getSenderUuid() {
        return sender;
    }
}
