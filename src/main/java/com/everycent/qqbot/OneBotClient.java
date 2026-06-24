package com.everycent.qqbot;

import com.everycent.config.QqBotProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class OneBotClient {

    private final QqBotProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public OneBotClient(QqBotProperties properties) {
        this.properties = properties;
    }

    public void sendReply(OneBotMessageEvent event, String message) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getOnebotBaseUrl()) || !StringUtils.hasText(message)) {
            return;
        }
        if ("group".equals(event.getMessageType()) && event.getGroupId() != null) {
            post("/send_group_msg", Map.of("group_id", event.getGroupId(), "message", message));
            return;
        }
        if ("private".equals(event.getMessageType()) && event.getUserId() != null) {
            post("/send_private_msg", Map.of("user_id", event.getUserId(), "message", message));
        }
    }

    private void post(String path, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(properties.getAccessToken())) {
            headers.setBearerAuth(properties.getAccessToken());
        }
        restTemplate.postForEntity(resolveUrl(path), new HttpEntity<>(new LinkedHashMap<>(payload), headers), String.class);
    }

    private String resolveUrl(String path) {
        String baseUrl = properties.getOnebotBaseUrl().replaceAll("/+$", "");
        return baseUrl + path;
    }
}
