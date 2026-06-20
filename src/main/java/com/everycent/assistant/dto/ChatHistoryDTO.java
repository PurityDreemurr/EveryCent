package com.everycent.assistant.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryDTO {

    private Long conversationId;

    private Long ledgerId;

    private List<ChatHistoryMessageDTO> messages = new ArrayList<>();

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public List<ChatHistoryMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatHistoryMessageDTO> messages) {
        this.messages = messages == null ? new ArrayList<>() : messages;
    }
}
