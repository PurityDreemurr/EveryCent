package com.everycent.service;

import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ExportDownloadTokenService {

    private static final String ENTITY_NAME = "export";
    private static final java.time.Duration TOKEN_TTL = java.time.Duration.ofMinutes(30);

    private final Map<String, ExportDownloadToken> tokens = new ConcurrentHashMap<>();

    public String create(Long userId, Long ledgerId, LocalDate startDate, LocalDate endDate) {
        cleanupExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new ExportDownloadToken(userId, ledgerId, startDate, endDate, Instant.now().plus(TOKEN_TTL)));
        return token;
    }

    public ExportDownloadToken resolve(String token) {
        cleanupExpired();
        ExportDownloadToken value = tokens.get(token);
        if (value == null || value.expiresAt().isBefore(Instant.now())) {
            tokens.remove(token);
            throw new BadRequestAlertException("Export download link is invalid or expired", ENTITY_NAME, "exporttokeninvalid");
        }
        return value;
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record ExportDownloadToken(Long userId, Long ledgerId, LocalDate startDate, LocalDate endDate, Instant expiresAt) {}
}
