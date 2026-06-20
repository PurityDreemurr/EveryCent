package com.everycent.assistant.dto;

public class AssistantResponseCardDTO {

    private String type;

    private String title;

    private String message;

    private Object data;

    public AssistantResponseCardDTO() {}

    public AssistantResponseCardDTO(String type, String title, String message, Object data) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
