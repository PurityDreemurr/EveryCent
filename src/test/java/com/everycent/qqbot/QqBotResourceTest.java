package com.everycent.qqbot;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.everycent.config.QqBotProperties;
import com.everycent.web.rest.QqBotResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class QqBotResourceTest {

    private final QqBotProperties properties = properties();
    private final QqBotAssistantBridgeService bridgeService = org.mockito.Mockito.mock(QqBotAssistantBridgeService.class);
    private final QqBotResource resource = new QqBotResource(properties, bridgeService);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAcceptOfficialEventWhenTokenMatches() {
        QqOfficialEvent event = new QqOfficialEvent();

        resource.handleOfficialEvent("secret", event);

        verify(bridgeService).handleEvent(event);
    }

    @Test
    void shouldRejectMissingToken() {
        assertThatThrownBy(() -> resource.handleOfficialEvent(null, new QqOfficialEvent())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectWhenCallbackTokenIsNotConfigured() {
        properties.setCallbackToken("");

        assertThatThrownBy(() -> resource.handleOfficialEvent("secret", new QqOfficialEvent())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldReturnOfficialValidationSignature() throws Exception {
        QqOfficialEvent event = new QqOfficialEvent();
        event.setOp(13);
        event.setD(objectMapper.readTree("{\"plain_token\":\"abc\",\"event_ts\":\"123\"}"));

        ResponseEntity<?> response = resource.handleOfficialEvent("secret", event);

        assertThat(response.getBody()).isInstanceOf(QqOfficialCallbackValidationResponse.class);
        QqOfficialCallbackValidationResponse body = (QqOfficialCallbackValidationResponse) response.getBody();
        assertThat(body.getPlainToken()).isEqualTo("abc");
        assertThat(body.getSignature()).isNotBlank();
        verifyNoInteractions(bridgeService);
    }

    private QqBotProperties properties() {
        QqBotProperties props = new QqBotProperties();
        props.setCallbackToken("secret");
        props.setAppSecret("app-secret");
        return props;
    }
}
