package com.everycent.qqbot;

import com.everycent.assistant.AiAssistantService;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.config.QqBotProperties;
import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import com.everycent.service.LedgerService;
import com.everycent.service.dto.LedgerDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final LedgerService ledgerService;
    private final Map<String, Long> currentLedgerIds = new ConcurrentHashMap<>();
    private final Map<String, List<LedgerDTO>> pendingLedgerSelections = new ConcurrentHashMap<>();

    public QqBotAssistantBridgeService(
        QqBotProperties properties,
        UserRepository userRepository,
        AiAssistantService aiAssistantService,
        QqOfficialBotClient qqOfficialBotClient,
        QqAssistantReplyRenderer replyRenderer,
        LedgerService ledgerService
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.aiAssistantService = aiAssistantService;
        this.qqOfficialBotClient = qqOfficialBotClient;
        this.replyRenderer = replyRenderer;
        this.ledgerService = ledgerService;
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
        User user = defaultUser();
        String senderKey = senderKey(event);
        LedgerSelection selection = parseLedgerSelection(text, senderKey);
        if (selection != null) {
            sendControlReply(event, switchLedger(user, senderKey, selection.ledger()));
            return;
        }
        if (isLedgerListRequest(text)) {
            sendControlReply(event, renderLedgerList(user, senderKey));
            return;
        }
        LedgerSwitchRequest switchRequest = parseLedgerSwitch(text);
        if (switchRequest != null) {
            sendControlReply(event, switchLedger(user, senderKey, switchRequest.ledgerName()));
            return;
        }
        if (properties.getDefaultLedgerId() == null || properties.getDefaultLedgerId() <= 0) {
            throw new BadRequestAlertException("QQ bot default ledger id is not configured", ENTITY_NAME, "defaultledgernotconfigured");
        }

        LedgerDTO currentLedger = currentLedger(user, senderKey);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setLedgerId(currentLedger.getId());
        request.setMessage(text);
        ChatResponseDTO response = aiAssistantService.chat(user, request);
        qqOfficialBotClient.sendReply(event, replyRenderer.render(response, currentLedger.getName(), isLedgerOperation(text, response)));
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

    private LedgerDTO currentLedger(User user, String senderKey) {
        Long ledgerId = currentLedgerIds.getOrDefault(senderKey, properties.getDefaultLedgerId());
        try {
            return ledgerService.findOne(user, ledgerId);
        } catch (RuntimeException e) {
            currentLedgerIds.remove(senderKey);
            return ledgerService.findOne(user, properties.getDefaultLedgerId());
        }
    }

    private String switchLedger(User user, String senderKey, String ledgerName) {
        List<LedgerDTO> ledgers = ledgerService.findLedgersForUser(user);
        LedgerDTO matched = ledgers
            .stream()
            .filter(ledger -> matchesLedgerName(ledger, ledgerName))
            .findFirst()
            .orElse(null);
        if (matched == null) {
            String available = ledgers
                .stream()
                .sorted(Comparator.comparing(LedgerDTO::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(ledger -> "「" + ledger.getName() + "」")
                .reduce((left, right) -> left + "、" + right)
                .orElse("暂无可用账本");
            return "没找到名为「" + ledgerName + "」的账本喵。当前可用账本：" + available + "。 {\"mood\":45,\"emoji\":\"calm\"}";
        }
        return switchLedger(user, senderKey, matched);
    }

    private String switchLedger(User user, String senderKey, LedgerDTO ledger) {
        if (ledger == null || ledger.getId() == null) {
            return "这个序号没有对应到可用账本，先重新问我账本列表再选一次喵。 {\"mood\":45,\"emoji\":\"calm\"}";
        }
        ledgerService.findOne(user, ledger.getId());
        currentLedgerIds.put(senderKey, ledger.getId());
        pendingLedgerSelections.remove(senderKey);
        return "已切换到当前账本「" + ledger.getName() + "」，接下来的记账和查账都会按这个账本行动喵。 {\"mood\":45,\"emoji\":\"pleased\"}";
    }

    private boolean matchesLedgerName(LedgerDTO ledger, String requestedName) {
        if (ledger == null || !StringUtils.hasText(ledger.getName()) || !StringUtils.hasText(requestedName)) {
            return false;
        }
        String ledgerName = ledger.getName().trim();
        String request = requestedName.trim();
        return ledgerName.equalsIgnoreCase(request) || ledgerName.contains(request) || request.contains(ledgerName);
    }

    private LedgerSwitchRequest parseLedgerSwitch(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (Pattern pattern : switchPatterns()) {
            Matcher matcher = pattern.matcher(text.trim());
            if (matcher.find()) {
                String name = normalizeLedgerName(matcher.group(1));
                if (StringUtils.hasText(name)) {
                    return new LedgerSwitchRequest(name);
                }
            }
        }
        return null;
    }

    private boolean isLedgerListRequest(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim();
        return (
            containsLedgerWord(normalized) &&
            (
                normalized.contains("哪些") ||
                normalized.contains("什么") ||
                normalized.contains("列表") ||
                normalized.contains("都有") ||
                normalized.contains("所有") ||
                normalized.contains("可用") ||
                normalized.contains("几个")
            )
        ) || normalized.matches("^(列出|查看|显示|看看).*[账帐][本单].*$");
    }

    private boolean containsLedgerWord(String text) {
        return StringUtils.hasText(text) && (text.contains("账本") || text.contains("账单") || text.contains("帐本") || text.contains("帐单"));
    }

    private String renderLedgerList(User user, String senderKey) {
        List<LedgerDTO> ledgers = ledgerService
            .findLedgersForUser(user)
            .stream()
            .toList();
        if (ledgers.isEmpty()) {
            pendingLedgerSelections.remove(senderKey);
            return "你现在还没有可用账本喵。 {\"mood\":40,\"emoji\":\"calm\"}";
        }
        pendingLedgerSelections.put(senderKey, ledgers);
        Long currentLedgerId = currentLedgerIds.get(senderKey);
        if (currentLedgerId == null && properties.getDefaultLedgerId() != null && properties.getDefaultLedgerId() > 0) {
            currentLedgerId = properties.getDefaultLedgerId();
        }
        List<String> lines = new java.util.ArrayList<>();
        lines.add("当前可用账本如下，回复序号就能切换喵：");
        for (int i = 0; i < ledgers.size(); i++) {
            LedgerDTO ledger = ledgers.get(i);
            String currentMark = java.util.Objects.equals(ledger.getId(), currentLedgerId) ? "（当前）" : "";
            lines.add((i + 1) + ". " + ledger.getName() + currentMark);
        }
        lines.add("{\"mood\":42,\"emoji\":\"calm\"}");
        return String.join("\n", lines);
    }

    private LedgerSelection parseLedgerSelection(String text, String senderKey) {
        if (!StringUtils.hasText(text) || !text.trim().matches("\\d{1,3}")) {
            return null;
        }
        List<LedgerDTO> ledgers = pendingLedgerSelections.get(senderKey);
        if (ledgers == null || ledgers.isEmpty()) {
            return null;
        }
        int index = Integer.parseInt(text.trim()) - 1;
        if (index < 0 || index >= ledgers.size()) {
            return new LedgerSelection(null);
        }
        return new LedgerSelection(ledgers.get(index));
    }

    private void sendControlReply(QqOfficialEvent event, String message) {
        qqOfficialBotClient.sendReply(event, cleanStateTail(message));
    }

    private String cleanStateTail(String message) {
        if (!StringUtils.hasText(message)) {
            return message;
        }
        return message.replaceAll("\\s*\\{\\s*\"mood\"\\s*:\\s*\\d+\\s*,\\s*\"emoji\"\\s*:\\s*\"[^\"]+\"\\s*}\\s*$", "").trim();
    }

    private List<Pattern> switchPatterns() {
        return List.of(
            Pattern.compile("切换(?:到|为)?[账帐][本单][「『“\\\"]?(.+?)[」』”\\\"]?$"),
            Pattern.compile("切换(?:到|为)[「『“\\\"]?(.+?)[」』”\\\"]?[账帐][本单]?$"),
            Pattern.compile("(?:更换|换)(?:到|为)?[账帐][本单][「『“\\\"]?(.+?)[」』”\\\"]?$"),
            Pattern.compile("(?:使用|用)[账帐][本单][「『“\\\"]?(.+?)[」』”\\\"]?$"),
            Pattern.compile("(?:使用|用)[「『“\\\"]?(.+?)[」』”\\\"]?[账帐][本单]$")
        );
    }

    private String normalizeLedgerName(String value) {
        return value == null
            ? ""
            : value
                .replaceAll("^(叫|名叫|名称是|为|到)", "")
                .replaceAll("(。|！|!|\\.)$", "")
                .trim();
    }

    private String senderKey(QqOfficialEvent event) {
        JsonNode data = event == null ? null : event.getD();
        if (data == null) {
            return "unknown";
        }
        String userOpenId = textAt(data, "author", "user_openid");
        if (StringUtils.hasText(userOpenId)) {
            return "user_openid:" + userOpenId;
        }
        String authorId = textAt(data, "author", "id");
        if (StringUtils.hasText(authorId)) {
            return "author:" + authorId;
        }
        String memberUserId = textAt(data, "member", "user", "id");
        if (StringUtils.hasText(memberUserId)) {
            return "member:" + memberUserId;
        }
        String messageId = data.path("id").asText();
        return StringUtils.hasText(messageId) ? "message:" + messageId : "unknown";
    }

    private String textAt(JsonNode node, String... path) {
        JsonNode current = node;
        for (String segment : path) {
            current = current == null ? null : current.path(segment);
        }
        return current == null || current.isMissingNode() || current.isNull() ? null : current.asText();
    }

    private boolean isLedgerOperation(String text, ChatResponseDTO response) {
        if (response != null && response.getCards() != null) {
            boolean hasLedgerCard = response
                .getCards()
                .stream()
                .anyMatch(card -> {
                    String type = card.getType();
                    return (
                        "transaction_created".equals(type) ||
                        "transaction_updated".equals(type) ||
                        "query_result".equals(type) ||
                        "budget_saved".equals(type) ||
                        "download_result".equals(type)
                    );
                });
            if (hasLedgerCard) {
                return true;
            }
        }
        return StringUtils.hasText(text) && (
            text.contains("账") ||
            text.contains("帐") ||
            text.contains("预算") ||
            text.contains("导出") ||
            text.contains("收入") ||
            text.contains("支出") ||
            text.contains("消费")
        );
    }

    private User defaultUser() {
        if (!StringUtils.hasText(properties.getDefaultUserLogin())) {
            throw new BadRequestAlertException("QQ bot default user login is not configured", ENTITY_NAME, "defaultusernotconfigured");
        }
        return userRepository
            .findOneByLogin(properties.getDefaultUserLogin())
            .orElseThrow(() -> new BadRequestAlertException("QQ bot default user not found", ENTITY_NAME, "defaultusernotfound"));
    }

    private record LedgerSwitchRequest(String ledgerName) {}

    private record LedgerSelection(LedgerDTO ledger) {}
}
