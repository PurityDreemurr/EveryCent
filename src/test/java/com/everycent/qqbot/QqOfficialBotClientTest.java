package com.everycent.qqbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.everycent.config.QqBotProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class QqOfficialBotClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldForceRefreshAccessToken() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QqOfficialBotClient client = new QqOfficialBotClient(properties(), restTemplate);

        expectToken(server, "token-1");
        expectToken(server, "token-2");

        assertThat(client.accessToken()).isEqualTo("token-1");
        assertThat(client.refreshAccessToken()).isEqualTo("token-2");

        server.verify();
    }

    @Test
    void shouldKeepCachedAccessTokenWhenForcedRefreshFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QqOfficialBotClient client = new QqOfficialBotClient(properties(), restTemplate);

        expectToken(server, "token-1");
        server
            .expect(requestTo("https://bots.qq.com/app/getAppAccessToken"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(client.accessToken()).isEqualTo("token-1");
        assertThatThrownBy(client::refreshAccessToken).isInstanceOf(RuntimeException.class);
        assertThat(client.accessToken()).isEqualTo("token-1");

        server.verify();
    }

    @Test
    void shouldRefreshAccessTokenAndRetryWhenSendReplyIsUnauthorized() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QqOfficialBotClient client = new QqOfficialBotClient(properties(), restTemplate);

        expectToken(server, "old-token");
        server
            .expect(requestTo("https://api.sgroup.qq.com/v2/users/user-1/messages"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "QQBot old-token"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectToken(server, "new-token");
        server
            .expect(requestTo("https://api.sgroup.qq.com/v2/users/user-1/messages"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "QQBot new-token"))
            .andRespond(withSuccess());

        client.sendReply(c2cMessage(), "收到喵");

        server.verify();
    }

    private void expectToken(MockRestServiceServer server, String token) {
        server
            .expect(requestTo("https://bots.qq.com/app/getAppAccessToken"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"access_token\":\"" + token + "\",\"expires_in\":7200}", MediaType.APPLICATION_JSON));
    }

    private QqOfficialEvent c2cMessage() throws Exception {
        QqOfficialEvent event = new QqOfficialEvent();
        event.setT("C2C_MESSAGE_CREATE");
        event.setD(objectMapper.readTree("{\"id\":\"msg-1\",\"content\":\"查账单\",\"author\":{\"user_openid\":\"user-1\"}}"));
        return event;
    }

    private QqBotProperties properties() {
        QqBotProperties properties = new QqBotProperties();
        properties.setEnabled(true);
        properties.setAppId("app-id");
        properties.setAppSecret("app-secret");
        properties.setApiBaseUrl("https://api.sgroup.qq.com");
        properties.setTokenBaseUrl("https://bots.qq.com");
        return properties;
    }
}
