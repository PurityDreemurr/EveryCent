package com.everycent.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_message")
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "conversation_id")
    private AiConversation conversation;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "ledger_id")
    private Ledger ledger;

    @NotNull
    @Size(max = 30)
    @Column(name = "role", length = 30, nullable = false)
    private String role;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @NotNull
    @Size(max = 50)
    @Column(name = "message_type", length = 50, nullable = false)
    private String messageType = "CHAT";

    @Size(max = 50)
    @Column(name = "response_type", length = 50)
    private String responseType;

    @Lob
    @Column(name = "cards_json")
    private String cardsJson;

    @Lob
    @Column(name = "skill_results_json")
    private String skillResultsJson;

    @Lob
    @Column(name = "accounting_capture_json")
    private String accountingCaptureJson;

    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;

    @ManyToOne
    @JoinColumn(name = "user_emotion_tag_id")
    private EmotionTag userEmotionTag;

    @Column(name = "user_emotion_confidence", precision = 5, scale = 4)
    private BigDecimal userEmotionConfidence;

    @Size(max = 50)
    @Column(name = "ai_emotion_before", length = 50)
    private String aiEmotionBefore;

    @Size(max = 50)
    @Column(name = "ai_emotion_after", length = 50)
    private String aiEmotionAfter;

    @ManyToOne
    @JoinColumn(name = "extracted_transaction_id")
    private TransactionRecord extractedTransaction;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AiConversation getConversation() {
        return conversation;
    }

    public void setConversation(AiConversation conversation) {
        this.conversation = conversation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
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

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getCardsJson() {
        return cardsJson;
    }

    public void setCardsJson(String cardsJson) {
        this.cardsJson = cardsJson;
    }

    public String getSkillResultsJson() {
        return skillResultsJson;
    }

    public void setSkillResultsJson(String skillResultsJson) {
        this.skillResultsJson = skillResultsJson;
    }

    public String getAccountingCaptureJson() {
        return accountingCaptureJson;
    }

    public void setAccountingCaptureJson(String accountingCaptureJson) {
        this.accountingCaptureJson = accountingCaptureJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public EmotionTag getUserEmotionTag() {
        return userEmotionTag;
    }

    public void setUserEmotionTag(EmotionTag userEmotionTag) {
        this.userEmotionTag = userEmotionTag;
    }

    public BigDecimal getUserEmotionConfidence() {
        return userEmotionConfidence;
    }

    public void setUserEmotionConfidence(BigDecimal userEmotionConfidence) {
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

    public TransactionRecord getExtractedTransaction() {
        return extractedTransaction;
    }

    public void setExtractedTransaction(TransactionRecord extractedTransaction) {
        this.extractedTransaction = extractedTransaction;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
}
