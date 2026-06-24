package com.everycent.qqbot;

import com.everycent.config.QqBotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class QqOfficialBotClient {

    private static final long TOKEN_REFRESH_SKEW_SECONDS = 60;

    private final QqBotProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedAccessToken;
    private Instant cachedAccessTokenExpiresAt = Instant.EPOCH;

    public QqOfficialBotClient(QqBotProperties properties) {
        this.properties = properties;
    }

    public void sendReply(QqOfficialEvent event, String message) {
        if (!properties.isEnabled() || !StringUtils.hasText(message) || event == null || event.getD() == null) {
            return;
        }
        JsonNode data = event.getD();
        String msgId = text(data, "id", text(data, "msg_id", null));
        String groupOpenId = text(data, "group_openid", null);
        if (StringUtils.hasText(groupOpenId)) {
            postMessage("/v2/groups/" + groupOpenId + "/messages", message, msgId);
            return;
        }
        String userOpenId = text(data, "user_openid", null);
        if (!StringUtils.hasText(userOpenId)) {
            userOpenId = text(data.path("author"), "user_openid", null);
        }
        if (StringUtils.hasText(userOpenId)) {
            postMessage("/v2/users/" + userOpenId + "/messages", message, msgId);
        }
    }

    public String gatewayUrl() {
        if (StringUtils.hasText(properties.getGatewayUrl())) {
            return properties.getGatewayUrl();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "QQBot " + accessToken());
        JsonNode response = restTemplate
            .exchange(resolveApiUrl("/gateway"), org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class)
            .getBody();
        String url = text(response, "url", null);
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("QQ official bot gateway response is empty");
        }
        return url;
    }

    public synchronized String accessToken() {
        return resolveAccessToken();
    }

    private void postMessage(String path, String content, String msgId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msg_type", 0);
        body.put("content", content);
        if (StringUtils.hasText(msgId)) {
            body.put("msg_id", msgId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "QQBot " + accessToken());
        restTemplate.postForEntity(resolveApiUrl(path), new HttpEntity<>(body, headers), String.class);
    }

    private String resolveAccessToken() {
        if (StringUtils.hasText(cachedAccessToken) && Instant.now().isBefore(cachedAccessTokenExpiresAt.minusSeconds(TOKEN_REFRESH_SKEW_SECONDS))) {
            return cachedAccessToken;
        }
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new IllegalStateException("QQ official bot app id or app secret is not configured");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appId", properties.getAppId());
        body.put("clientSecret", properties.getAppSecret());
        JsonNode response = restTemplate.postForObject(resolveTokenUrl("/app/getAppAccessToken"), body, JsonNode.class);
        cachedAccessToken = text(response, "access_token", null);
        long expiresIn = response == null || !response.path("expires_in").canConvertToLong() ? 7200 : response.path("expires_in").asLong();
        cachedAccessTokenExpiresAt = Instant.now().plusSeconds(expiresIn);
        if (!StringUtils.hasText(cachedAccessToken)) {
            throw new IllegalStateException("QQ official bot access token response is empty");
        }
        return cachedAccessToken;
    }

    private String resolveApiUrl(String path) {
        return trimTrailingSlash(properties.getApiBaseUrl()) + path;
    }

    private String resolveTokenUrl(String path) {
        return trimTrailingSlash(properties.getTokenBaseUrl()) + path;
    }

    private String trimTrailingSlash(String value) {
        return (StringUtils.hasText(value) ? value : "").replaceAll("/+$", "");
    }

    private String text(JsonNode node, String field, String defaultValue) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return defaultValue;
        }
        String value = node.path(field).asText();
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
