package com.everycent.assistant.skill;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillResult {

    private Boolean success = false;

    private String actionName;

    private Object data;

    private String errorCode;

    private String message;

    private Boolean needUserConfirmation = false;

    private Boolean blockedByPolicy = false;

    public static SkillResult success(String actionName, Object data) {
        SkillResult result = new SkillResult();
        result.setSuccess(true);
        result.setActionName(actionName);
        result.setData(data);
        return result;
    }

    public static SkillResult failure(String actionName, String errorCode, String message) {
        SkillResult result = new SkillResult();
        result.setActionName(actionName);
        result.setErrorCode(errorCode);
        result.setMessage(message);
        return result;
    }

    public static SkillResult needConfirmation(String actionName, Object data, String message) {
        SkillResult result = new SkillResult();
        result.setActionName(actionName);
        result.setData(data);
        result.setMessage(message);
        result.setNeedUserConfirmation(true);
        return result;
    }

    public static SkillResult blocked(String actionName, String message) {
        SkillResult result = failure(actionName, "ACTION_FORBIDDEN", message);
        result.setBlockedByPolicy(true);
        return result;
    }

    public static Map<String, Object> dataEntry(String key, Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(key, value);
        return data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
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
