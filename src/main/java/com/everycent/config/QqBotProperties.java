package com.everycent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.qq-bot")
public class QqBotProperties {

    private boolean enabled = false;

    private String onebotBaseUrl;

    private String accessToken;

    private String webhookToken;

    private String appId;

    private String appSecret;

    private String apiBaseUrl = "https://api.sgroup.qq.com";

    private String tokenBaseUrl = "https://bots.qq.com";

    private String gatewayUrl;

    private Integer gatewayIntents = 33554432;

    private String callbackToken;

    private String defaultUserLogin = "admin";

    private Long defaultLedgerId;

    private String publicBaseUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOnebotBaseUrl() {
        return onebotBaseUrl;
    }

    public void setOnebotBaseUrl(String onebotBaseUrl) {
        this.onebotBaseUrl = onebotBaseUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getWebhookToken() {
        return webhookToken;
    }

    public void setWebhookToken(String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getTokenBaseUrl() {
        return tokenBaseUrl;
    }

    public void setTokenBaseUrl(String tokenBaseUrl) {
        this.tokenBaseUrl = tokenBaseUrl;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public Integer getGatewayIntents() {
        return gatewayIntents;
    }

    public void setGatewayIntents(Integer gatewayIntents) {
        this.gatewayIntents = gatewayIntents;
    }

    public String getCallbackToken() {
        return callbackToken;
    }

    public void setCallbackToken(String callbackToken) {
        this.callbackToken = callbackToken;
    }

    public String getDefaultUserLogin() {
        return defaultUserLogin;
    }

    public void setDefaultUserLogin(String defaultUserLogin) {
        this.defaultUserLogin = defaultUserLogin;
    }

    public Long getDefaultLedgerId() {
        return defaultLedgerId;
    }

    public void setDefaultLedgerId(Long defaultLedgerId) {
        this.defaultLedgerId = defaultLedgerId;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
