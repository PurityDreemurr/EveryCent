package com.everycent.service.dto;

public class QqBotBindingDTO {

    private String bindingCode;

    private boolean bound;

    private String qqOpenIdMask;

    public String getBindingCode() {
        return bindingCode;
    }

    public void setBindingCode(String bindingCode) {
        this.bindingCode = bindingCode;
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    public String getQqOpenIdMask() {
        return qqOpenIdMask;
    }

    public void setQqOpenIdMask(String qqOpenIdMask) {
        this.qqOpenIdMask = qqOpenIdMask;
    }
}
