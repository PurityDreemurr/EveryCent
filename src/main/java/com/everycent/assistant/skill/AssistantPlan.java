package com.everycent.assistant.skill;

import java.util.ArrayList;
import java.util.List;

public class AssistantPlan {

    private AssistantIntent intent = AssistantIntent.UNKNOWN;

    private Double confidence;

    private Boolean needUserReply = false;

    private Boolean needConfirmation = false;

    private Boolean blockedByPolicy = false;

    private List<AssistantAction> actions = new ArrayList<>();

    private ReplyStyle replyStyle = new ReplyStyle();

    public boolean hasOnlyAllowedActions() {
        return actions != null && actions.stream().allMatch(action -> action != null && action.isAllowed());
    }

    public boolean hasForbiddenAction() {
        return actions != null && actions.stream().anyMatch(action -> action != null && action.isForbidden());
    }

    public AssistantIntent getIntent() {
        return intent;
    }

    public void setIntent(AssistantIntent intent) {
        this.intent = intent;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getNeedUserReply() {
        return needUserReply;
    }

    public void setNeedUserReply(Boolean needUserReply) {
        this.needUserReply = needUserReply;
    }

    public Boolean getNeedConfirmation() {
        return needConfirmation;
    }

    public void setNeedConfirmation(Boolean needConfirmation) {
        this.needConfirmation = needConfirmation;
    }

    public Boolean getBlockedByPolicy() {
        return blockedByPolicy;
    }

    public void setBlockedByPolicy(Boolean blockedByPolicy) {
        this.blockedByPolicy = blockedByPolicy;
    }

    public List<AssistantAction> getActions() {
        return actions;
    }

    public void setActions(List<AssistantAction> actions) {
        this.actions = actions == null ? new ArrayList<>() : actions;
    }

    public ReplyStyle getReplyStyle() {
        return replyStyle;
    }

    public void setReplyStyle(ReplyStyle replyStyle) {
        this.replyStyle = replyStyle;
    }
}
