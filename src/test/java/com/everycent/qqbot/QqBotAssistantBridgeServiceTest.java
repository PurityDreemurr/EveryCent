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
import com.everycent.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QqBotAssistantBridgeServiceTest {

    private final QqBotProperties properties = properties();
    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final AiAssistantService aiAssistantService = org.mockito.Mockito.mock(AiAssistantService.class);
    private final QqOfficialBotClient qqOfficialBotClient = org.mockito.Mockito.mock(QqOfficialBotClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QqAssistantReplyRenderer replyRenderer = new QqAssistantReplyRenderer(objectMapper);
    private final QqBotAssistantBridgeService service = new QqBotAssistantBridgeService(
        properties,
        userRepository,
        aiAssistantService,
        qqOfficialBotClient,
        replyRenderer
    );

    @Test
    void shouldSendAssistantRawMessageBackToQqOfficialBot() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(userRepository.findOneByLogin("admin")).thenReturn(Optional.of(user));
        ChatResponseDTO response = new ChatResponseDTO();
        response.setAssistantMessage("已为您查询账单。{\"mood\":50,\"emoji\":\"peace\"}");
        when(aiAssistantService.chat(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(ChatRequestDTO.class))).thenReturn(response);

        QqOfficialEvent event = c2cMessage("查账单");
        service.handleEvent(event);

        ArgumentCaptor<ChatRequestDTO> requestCaptor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(aiAssistantService).chat(org.mockito.ArgumentMatchers.eq(user), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getLedgerId()).isEqualTo(10L);
        assertThat(requestCaptor.getValue().getMessage()).isEqualTo("查账单");
        verify(qqOfficialBotClient).sendReply(event, "已为您查询账单。");
    }

    @Test
    void shouldRenderQueryCardDataForQqReply() throws Exception {
        User user = new User();
        user.setLogin("admin");
        when(userRepository.findOneByLogin("admin")).thenReturn(Optional.of(user));
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
        assertThat(replyCaptor.getValue()).contains("账单明细").contains("午餐").contains("¥20.00").contains("餐饮");
    }

    @Test
    void shouldIgnoreNonMessageEvent() {
        QqOfficialEvent event = new QqOfficialEvent();
        event.setT("READY");

        service.handleEvent(event);

        verifyNoInteractions(userRepository, aiAssistantService, qqOfficialBotClient);
    }

    private QqOfficialEvent c2cMessage(String text) throws Exception {
        QqOfficialEvent event = new QqOfficialEvent();
        event.setT("C2C_MESSAGE_CREATE");
        event.setD(objectMapper.readTree("{\"id\":\"msg-1\",\"content\":\"" + text + "\",\"author\":{\"user_openid\":\"openid-1\"}}"));
        return event;
    }

    private QqBotProperties properties() {
        QqBotProperties props = new QqBotProperties();
        props.setEnabled(true);
        props.setDefaultUserLogin("admin");
        props.setDefaultLedgerId(10L);
        return props;
    }
}
