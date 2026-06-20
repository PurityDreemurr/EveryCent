package com.everycent.assistant.dto;

import com.everycent.assistant.skill.SkillResult;

public class AssistantSkillResultDTO {

    private String actionName;

    private Boolean success;

    private String errorCode;

    private String message;

    private Boolean needUserConfirmation;

    private Boolean blockedByPolicy;

    public static AssistantSkillResultDTO from(SkillResult result) {
        AssistantSkillResultDTO dto = new AssistantSkillResultDTO();
        if (result == null) {
            dto.setSuccess(false);
            dto.setErrorCode("SKILL_EMPTY_RESULT");
            return dto;
        }
        dto.setActionName(result.getActionName());
        dto.setSuccess(result.getSuccess());
        dto.setErrorCode(result.getErrorCode());
        dto.setMessage(result.getMessage());
        dto.setNeedUserConfirmation(result.getNeedUserConfirmation());
        dto.setBlockedByPolicy(result.getBlockedByPolicy());
        return dto;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getNeedUserConfirmation() {
        return needUserConfirmation;
    }

    public void setNeedUserConfirmation(Boolean needUserConfirmation) {
        this.needUserConfirmation = needUserConfirmation;
    }

    public Boolean getBlockedByPolicy() {
        return blockedByPolicy;
    }

    public void setBlockedByPolicy(Boolean blockedByPolicy) {
        this.blockedByPolicy = blockedByPolicy;
    }
}
