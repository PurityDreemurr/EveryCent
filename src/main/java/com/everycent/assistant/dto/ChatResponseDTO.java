package com.everycent.assistant.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatResponseDTO {

    private Long conversationId;

    private Long messageId;

    private String assistantMessage;

    private String userEmotionTagCode;

    private Double userEmotionConfidence;

    private String aiEmotionBefore;

    private String aiEmotionAfter;

    private AccountingCaptureDTO accountingCapture;

    private String responseType;

    private List<AssistantResponseCardDTO> cards = new ArrayList<>();

    private List<AssistantSkillResultDTO> skillResults = new ArrayList<>();

    private List<MemoryContextDTO> retrievedMemories = new ArrayList<>();

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public String getUserEmotionTagCode() {
        return userEmotionTagCode;
    }

    public void setUserEmotionTagCode(String userEmotionTagCode) {
        this.userEmotionTagCode = userEmotionTagCode;
    }

    public Double getUserEmotionConfidence() {
        return userEmotionConfidence;
    }

    public void setUserEmotionConfidence(Double userEmotionConfidence) {
        this.userEmotionConfidence = userEmotionConfidence;
    }

    public String getAiEmotionBefore() {
        return aiEmotionBefore;
    }

    public void setAiEmotionBefore(String aiEmotionBefore) {
        this.aiEmotionBefore = aiEmotionBefore;
    }

    public String getAiEmotionAfter() {
        return aiEmotionAfter;
    }

    public void setAiEmotionAfter(String aiEmotionAfter) {
        this.aiEmotionAfter = aiEmotionAfter;
    }

    public AccountingCaptureDTO getAccountingCapture() {
        return accountingCapture;
    }

    public void setAccountingCapture(AccountingCaptureDTO accountingCapture) {
        this.accountingCapture = accountingCapture;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public List<AssistantResponseCardDTO> getCards() {
        return cards;
    }

    public void setCards(List<AssistantResponseCardDTO> cards) {
        this.cards = cards == null ? new ArrayList<>() : cards;
    }

    public List<AssistantSkillResultDTO> getSkillResults() {
        return skillResults;
    }

    public void setSkillResults(List<AssistantSkillResultDTO> skillResults) {
        this.skillResults = skillResults == null ? new ArrayList<>() : skillResults;
    }

    public List<MemoryContextDTO> getRetrievedMemories() {
        return retrievedMemories;
    }

    public void setRetrievedMemories(List<MemoryContextDTO> retrievedMemories) {
        this.retrievedMemories = retrievedMemories;
    }
}
