package com.everycent.qqbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.everycent.assistant.AiAssistantService;
import com.everycent.assistant.dto.AssistantResponseCardDTO;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.config.QqBotProperties;
import com.everycent.domain.User;
import com.everycent.service.LedgerService;
import com.everycent.service.QqBotBindingService;
import com.everycent.service.dto.LedgerDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QqBotAssistantBridgeServiceTest {

    private final QqBotProperties properties = properties();
    private final QqBotBindingService qqBotBindingService = org.mockito.Mockito.mock(QqBotBindingService.class);
    private final AiAssistantService aiAssistantService = org.mockito.Mockito.mock(AiAssistantService.class);
    private final QqOfficialBotClient qqOfficialBotClient = org.mockito.Mockito.mock(QqOfficialBotClient.class);
    private final LedgerService ledgerService = org.mockito.Mockito.mock(LedgerService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QqAssistantReplyRenderer replyRenderer = new QqAssistantReplyRenderer(objectMapper);
    private final QqBotAssistantBridgeService service = new QqBotAssistantBridgeService(
        properties,
        qqBotBindingService,
        aiAssistantService,
        qqOfficialBotClient,
        replyRenderer,
        ledgerService
    );

    @Test
    void shouldSendAssistantRawMessageBackToQqOfficialBot() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findOne(user, 10L)).thenReturn(ledger(10L, "日常账本"));
        ChatResponseDTO response = new ChatResponseDTO();
        response.setAssistantMessage("已为您查询账单。{\"mood\":50,\"emoji\":\"peace\"}");
        when(aiAssistantService.chat(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(ChatRequestDTO.class))).thenReturn(response);

        QqOfficialEvent event = c2cMessage("查账单");
        service.handleEvent(event);

        ArgumentCaptor<ChatRequestDTO> requestCaptor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(aiAssistantService).chat(org.mockito.ArgumentMatchers.eq(user), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getLedgerId()).isEqualTo(10L);
        assertThat(requestCaptor.getValue().getMessage()).isEqualTo("查账单");
        verify(qqOfficialBotClient).sendReply(event, "已为您查询账单。\n\n当前账本：日常账本");
    }

    @Test
    void shouldRenderQueryCardDataForQqReply() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findOne(user, 10L)).thenReturn(ledger(10L, "日常账本"));
        ChatResponseDTO response = new ChatResponseDTO();
        response.setAssistantMessage("已查询账单。");
        response.setCards(
            java.util.List.of(
                new AssistantResponseCardDTO(
                    "query_result",
                    "查询结果",
                    "已查到相关结果。",
                    objectMapper.readTree(
                        """
                        {
                          "totalElements": 1,
                          "content": [
                            {
                              "recordDate": "2026-06-24",
                              "type": "EXPENSE",
                              "description": "午餐",
                              "amount": "20.00",
                              "behaviorTagName": "餐饮"
                            }
                          ]
                        }
                        """
                    )
                )
            )
        );
        when(aiAssistantService.chat(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(ChatRequestDTO.class))).thenReturn(response);

        QqOfficialEvent event = c2cMessage("查账单");
        service.handleEvent(event);

        ArgumentCaptor<String> replyCaptor = ArgumentCaptor.forClass(String.class);
        verify(qqOfficialBotClient).sendReply(org.mockito.ArgumentMatchers.eq(event), replyCaptor.capture());
        assertThat(replyCaptor.getValue()).contains("当前账本：日常账本").contains("账单明细").contains("午餐").contains("¥20.00").contains("餐饮");
    }

    @Test
    void shouldRenderAllTransactionRowsForQqReply() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findOne(user, 10L)).thenReturn(ledger(10L, "日常账本"));
        ChatResponseDTO response = new ChatResponseDTO();
        response.setAssistantMessage("已查询账单。");
        response.setCards(
            java.util.List.of(new AssistantResponseCardDTO("query_result", "查询结果", "已查到相关结果。", transactionPage(12)))
        );
        when(aiAssistantService.chat(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(ChatRequestDTO.class))).thenReturn(response);

        QqOfficialEvent event = c2cMessage("查账单");
        service.handleEvent(event);

        ArgumentCaptor<String> replyCaptor = ArgumentCaptor.forClass(String.class);
        verify(qqOfficialBotClient).sendReply(org.mockito.ArgumentMatchers.eq(event), replyCaptor.capture());
        assertThat(replyCaptor.getValue()).contains("账单明细：共 12 条").contains("12. 2026-06-12 支出 第12笔 ¥12.00 [餐饮]");
        assertThat(replyCaptor.getValue()).doesNotContain("前端查看");
    }

    @Test
    void shouldRenderExportDownloadLinkForQqReply() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findOne(user, 10L)).thenReturn(ledger(10L, "日常账本"));
        ChatResponseDTO response = new ChatResponseDTO();
        response.setAssistantMessage("导出结果准备好了，链接在下面喵。{\"mood\":42,\"emoji\":\"calm\"}");
        response.setCards(
            List.of(
                new AssistantResponseCardDTO(
                    "download_result",
                    "导出已准备",
                    "账单导出已准备好。",
                    Map.of("downloadUrl", "https://everycent.example.com/api/public/exports/transactions/token-1", "byteLength", 3)
                )
            )
        );
        when(aiAssistantService.chat(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(ChatRequestDTO.class))).thenReturn(response);

        QqOfficialEvent event = c2cMessage("导出本月账单");
        service.handleEvent(event);

        ArgumentCaptor<String> replyCaptor = ArgumentCaptor.forClass(String.class);
        verify(qqOfficialBotClient).sendReply(org.mockito.ArgumentMatchers.eq(event), replyCaptor.capture());
        assertThat(replyCaptor.getValue())
            .contains("下载链接：https://everycent.example.com/api/public/exports/transactions/token-1")
            .doesNotContain("byteLength")
            .doesNotContain("/api/ledgers/10/transactions/export");
    }

    @Test
    void shouldSwitchLedgerByNaturalLanguageAndUseItForNextRequest() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findLedgersForUser(user)).thenReturn(List.of(ledger(10L, "日常账本"), ledger(20L, "旅行账本")));
        when(ledgerService.findOne(user, 10L)).thenReturn(ledger(10L, "日常账本"));
        when(ledgerService.findOne(user, 20L)).thenReturn(ledger(20L, "旅行账本"));
        ChatResponseDTO response = new ChatResponseDTO();
        response.setAssistantMessage("账单查好了，明细我已经排在下面喵。{\"mood\":42,\"emoji\":\"pleased\"}");
        when(aiAssistantService.chat(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(ChatRequestDTO.class))).thenReturn(response);

        QqOfficialEvent switchEvent = c2cMessage("切换到旅行账本");
        service.handleEvent(switchEvent);
        QqOfficialEvent queryEvent = c2cMessage("查账单");
        service.handleEvent(queryEvent);

        verify(qqOfficialBotClient)
            .sendReply(org.mockito.ArgumentMatchers.eq(switchEvent), org.mockito.ArgumentMatchers.contains("已切换到当前账本「旅行账本」"));
        ArgumentCaptor<ChatRequestDTO> requestCaptor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(aiAssistantService).chat(org.mockito.ArgumentMatchers.eq(user), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getLedgerId()).isEqualTo(20L);
        verify(qqOfficialBotClient)
            .sendReply(org.mockito.ArgumentMatchers.eq(queryEvent), org.mockito.ArgumentMatchers.contains("当前账本：旅行账本"));
    }

    @Test
    void shouldListLedgersAndSwitchByReplyingIndex() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findLedgersForUser(user)).thenReturn(List.of(ledger(10L, "日常账本"), ledger(20L, "旅行账本")));
        when(ledgerService.findOne(user, 10L)).thenReturn(ledger(10L, "日常账本"));
        when(ledgerService.findOne(user, 20L)).thenReturn(ledger(20L, "旅行账本"));
        ChatResponseDTO response = new ChatResponseDTO();
        response.setAssistantMessage("账单查好了，明细我已经排在下面喵。{\"mood\":42,\"emoji\":\"pleased\"}");
        when(aiAssistantService.chat(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(ChatRequestDTO.class))).thenReturn(response);

        QqOfficialEvent listEvent = c2cMessage("当前都有什么账本");
        service.handleEvent(listEvent);
        QqOfficialEvent selectEvent = c2cMessage("2");
        service.handleEvent(selectEvent);
        QqOfficialEvent queryEvent = c2cMessage("查账单");
        service.handleEvent(queryEvent);

        verify(qqOfficialBotClient)
            .sendReply(
                org.mockito.ArgumentMatchers.eq(listEvent),
                org.mockito.ArgumentMatchers.argThat(reply -> reply.contains("1. 日常账本") && reply.contains("2. 旅行账本"))
            );
        verify(qqOfficialBotClient)
            .sendReply(org.mockito.ArgumentMatchers.eq(selectEvent), org.mockito.ArgumentMatchers.contains("已切换到当前账本「旅行账本」"));
        ArgumentCaptor<ChatRequestDTO> requestCaptor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(aiAssistantService).chat(org.mockito.ArgumentMatchers.eq(user), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getLedgerId()).isEqualTo(20L);
        verify(qqOfficialBotClient)
            .sendReply(org.mockito.ArgumentMatchers.eq(queryEvent), org.mockito.ArgumentMatchers.contains("当前账本：旅行账本"));
    }

    @Test
    void shouldRecognizeLedgerTypoForListAndSwitchCommands() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findLedgersForUser(user)).thenReturn(List.of(ledger(10L, "日常账本"), ledger(20L, "旅行账本")));
        when(ledgerService.findOne(user, 20L)).thenReturn(ledger(20L, "旅行账本"));

        QqOfficialEvent listEvent = c2cMessage("当前都有什么帐本");
        service.handleEvent(listEvent);
        QqOfficialEvent switchEvent = c2cMessage("切换到旅行帐本");
        service.handleEvent(switchEvent);

        verify(qqOfficialBotClient)
            .sendReply(
                org.mockito.ArgumentMatchers.eq(listEvent),
                org.mockito.ArgumentMatchers.argThat(reply -> reply.contains("1. 日常账本") && reply.contains("2. 旅行账本"))
            );
        verify(qqOfficialBotClient)
            .sendReply(org.mockito.ArgumentMatchers.eq(switchEvent), org.mockito.ArgumentMatchers.contains("已切换到当前账本「旅行账本」"));
    }

    @Test
    void shouldListLedgersEvenWhenDefaultLedgerIsNotConfigured() throws Exception {
        properties.setDefaultLedgerId(0L);
        User user = new User();
        user.setLogin("admin");
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.of(user));
        when(ledgerService.findLedgersForUser(user)).thenReturn(List.of(ledger(10L, "日常账本"), ledger(20L, "旅行账本")));

        QqOfficialEvent listEvent = c2cMessage("账本列表");
        service.handleEvent(listEvent);

        verify(qqOfficialBotClient)
            .sendReply(
                org.mockito.ArgumentMatchers.eq(listEvent),
                org.mockito.ArgumentMatchers.argThat(reply ->
                    reply.contains("当前可用账本如下") &&
                    reply.contains("1. 日常账本") &&
                    reply.contains("2. 旅行账本") &&
                    !reply.contains("\"mood\"")
                )
            );
        verifyNoInteractions(aiAssistantService);
    }

    @Test
    void shouldPromptWhenQqOpenIdIsNotBound() throws Exception {
        when(qqBotBindingService.findBoundUser("openid-1")).thenReturn(Optional.empty());

        QqOfficialEvent event = c2cMessage("查账单");
        service.handleEvent(event);

        verify(qqOfficialBotClient)
            .sendReply(
                org.mockito.ArgumentMatchers.eq(event),
                org.mockito.ArgumentMatchers.argThat(reply -> reply.contains("还没有绑定 EveryCent 账号") && reply.contains("ECQQ-XXXXXXXX"))
            );
        verifyNoInteractions(aiAssistantService, ledgerService);
    }

    @Test
    void shouldBindQqOpenIdWithBindingCode() throws Exception {
        when(qqBotBindingService.bind("ECQQ-ABCD2345", "openid-1"))
            .thenReturn(QqBotBindingService.BindResult.success("绑定成功喵，接下来我会使用 EveryCent 账号「alice」处理记账和查账。"));

        QqOfficialEvent event = c2cMessage("绑定 ECQQ-ABCD2345");
        service.handleEvent(event);

        verify(qqBotBindingService).bind("ECQQ-ABCD2345", "openid-1");
        verify(qqOfficialBotClient).sendReply(org.mockito.ArgumentMatchers.eq(event), org.mockito.ArgumentMatchers.contains("绑定成功"));
        verifyNoInteractions(aiAssistantService, ledgerService);
    }

    @Test
    void shouldIgnoreNonMessageEvent() {
        QqOfficialEvent event = new QqOfficialEvent();
        event.setT("READY");

        service.handleEvent(event);

        verifyNoInteractions(qqBotBindingService, aiAssistantService, qqOfficialBotClient, ledgerService);
    }

    private QqOfficialEvent c2cMessage(String text) throws Exception {
        QqOfficialEvent event = new QqOfficialEvent();
        event.setT("C2C_MESSAGE_CREATE");
        event.setD(objectMapper.readTree("{\"id\":\"msg-1\",\"content\":\"" + text + "\",\"author\":{\"user_openid\":\"openid-1\"}}"));
        return event;
    }

    private ObjectNode transactionPage(int count) {
        ObjectNode page = objectMapper.createObjectNode();
        page.put("totalElements", count);
        ArrayNode content = page.putArray("content");
        for (int i = 1; i <= count; i++) {
            ObjectNode record = content.addObject();
            record.put("recordDate", "2026-06-" + String.format("%02d", i));
            record.put("type", "EXPENSE");
            record.put("description", "第" + i + "笔");
            record.put("amount", String.valueOf(i));
            record.put("behaviorTagName", "餐饮");
        }
        return page;
    }

    private LedgerDTO ledger(Long id, String name) {
        LedgerDTO ledger = new LedgerDTO();
        ledger.setId(id);
        ledger.setName(name);
        return ledger;
    }

    private QqBotProperties properties() {
        QqBotProperties props = new QqBotProperties();
        props.setEnabled(true);
        props.setDefaultUserLogin("admin");
        props.setDefaultLedgerId(10L);
        return props;
    }
}
