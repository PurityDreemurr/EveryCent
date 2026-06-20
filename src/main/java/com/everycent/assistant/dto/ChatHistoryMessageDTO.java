package com.everycent.assistant.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryMessageDTO {

    private String id;

    private String role;

    private String content;

    private String createdAt;

    private List<AssistantResponseCardDTO> cards = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<AssistantResponseCardDTO> getCards() {
        return cards;
    }

    public void setCards(List<AssistantResponseCardDTO> cards) {
        this.cards = cards == null ? new ArrayList<>() : cards;
    }
}
