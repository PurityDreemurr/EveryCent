package com.everycent.web.rest;

import com.everycent.config.QqBotProperties;
import com.everycent.qqbot.QqOfficialCallbackValidationResponse;
import com.everycent.qqbot.QqOfficialEvent;
import com.everycent.qqbot.QqBotAssistantBridgeService;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/qq-bot")
public class QqBotResource {

    private final QqBotProperties properties;
    private final QqBotAssistantBridgeService bridgeService;

    public QqBotResource(QqBotProperties properties, QqBotAssistantBridgeService bridgeService) {
        this.properties = properties;
        this.bridgeService = bridgeService;
    }

    @PostMapping("/official/events")
    public ResponseEntity<?> handleOfficialEvent(
        @RequestParam(value = "token", required = false) String token,
        @RequestBody QqOfficialEvent event
    ) {
        assertCallbackToken(token);
        if (event != null && Integer.valueOf(13).equals(event.getOp())) {
            return ResponseEntity.ok(validationResponse(event));
        }
        bridgeService.handleEvent(event);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private void assertCallbackToken(String token) {
        if (!StringUtils.hasText(properties.getCallbackToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QQ bot callback token is not configured");
        }
        if (!properties.getCallbackToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid QQ bot callback token");
        }
    }

    private QqOfficialCallbackValidationResponse validationResponse(QqOfficialEvent event) {
        if (event.getD() == null || !StringUtils.hasText(properties.getAppSecret())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QQ bot validation payload or app secret is missing");
        }
        String plainToken = event.getD().path("plain_token").asText(null);
        String eventTs = event.getD().path("event_ts").asText(null);
        if (!StringUtils.hasText(plainToken) || !StringUtils.hasText(eventTs)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QQ bot validation payload is invalid");
        }
        return new QqOfficialCallbackValidationResponse(plainToken, hmacSha256Hex(eventTs + plainToken, properties.getAppSecret()));
    }

    private String hmacSha256Hex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "QQ bot validation signature failed", e);
        }
    }
}
