package com.everycent.qqbot;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QqOfficialCallbackValidationResponse {

    @JsonProperty("plain_token")
    private String plainToken;

    private String signature;

    public QqOfficialCallbackValidationResponse(String plainToken, String signature) {
        this.plainToken = plainToken;
        this.signature = signature;
    }

    public String getPlainToken() {
        return plainToken;
    }

    public void setPlainToken(String plainToken) {
        this.plainToken = plainToken;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
