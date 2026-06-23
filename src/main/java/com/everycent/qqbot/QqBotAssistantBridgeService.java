package com.everycent.qqbot;

import com.everycent.assistant.AiAssistantService;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.config.QqBotProperties;
import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import com.everycent.web.rest.errors.BadRequestAlertException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class QqBotAssistantBridgeService {

    private static final Logger LOG = LoggerFactory.getLogger(QqBotAssistantBridgeService.class);
    private static final String ENTITY_NAME = "qqBot";

    private final QqBotProperties properties;
    private final UserRepository userRepository;
    private final AiAssistantService aiAssistantService;
    private final QqOfficialBotClient qqOfficialBotClient;
    private final QqAssistantReplyRenderer replyRenderer;

    public QqBotAssistantBridgeService(
        QqBotProperties properties,
        UserRepository userRepository,
        AiAssistantService aiAssistantService,
        QqOfficialBotClient qqOfficialBotClient,
        QqAssistantReplyRenderer replyRenderer
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.aiAssistantService = aiAssistantService;
        this.qqOfficialBotClient = qqOfficialBotClient;
        this.replyRenderer = replyRenderer;
    }

    public void handleEvent(QqOfficialEvent event) {
        if (!properties.isEnabled()) {
            LOG.debug("QQ bot bridge is disabled; event ignored.");
            return;
        }
        if (!isSupportedMessage(event)) {
            return;
        }

        String text = messageText(event);
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (properties.getDefaultLedgerId() == null || properties.getDefaultLedgerId() <= 0) {
            throw new BadRequestAlertException("QQ bot default ledger id is not configured", ENTITY_NAME, "defaultledgernotconfigured");
        }

        ChatRequestDTO request = new ChatRequestDTO();
        request.setLedgerId(properties.getDefaultLedgerId());
        request.setMessage(text);
        ChatResponseDTO response = aiAssistantService.chat(defaultUser(), request);
        qqOfficialBotClient.sendReply(event, replyRenderer.render(response));
    }

    private boolean isSupportedMessage(QqOfficialEvent event) {
        return event != null && event.getD() != null && StringUtils.hasText(messageText(event)) && isMessageEventType(event.getT());
    }

    private boolean isMessageEventType(String eventType) {
        return (
            "C2C_MESSAGE_CREATE".equals(eventType) ||
            "GROUP_AT_MESSAGE_CREATE".equals(eventType) ||
            "AT_MESSAGE_CREATE".equals(eventType) ||
            "MESSAGE_CREATE".equals(eventType)
        );
    }

    private String messageText(QqOfficialEvent event) {
        JsonNode data = event == null ? null : event.getD();
        if (data == null || data.path("content").isMissingNode() || data.path("content").isNull()) {
            return null;
        }
        return data.path("content").asText().trim();
    }

    private User defaultUser() {
        if (!StringUtils.hasText(properties.getDefaultUserLogin())) {
            throw new BadRequestAlertException("QQ bot default user login is not configured", ENTITY_NAME, "defaultusernotconfigured");
        }
        return userRepository
            .findOneByLogin(properties.getDefaultUserLogin())
            .orElseThrow(() -> new BadRequestAlertException("QQ bot default user not found", ENTITY_NAME, "defaultusernotfound"));
    }
}
