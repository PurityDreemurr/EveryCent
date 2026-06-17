package com.everycent.web.rest.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

import com.everycent.llm.client.LlmClientException;
import com.everycent.llm.parser.LlmParseException;
import com.everycent.service.InvalidAiResultException;
import com.everycent.service.NoLedgerPermissionException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;

class ExceptionTranslatorLlmTest {

    private final ExceptionTranslator translator = new ExceptionTranslator(new StandardEnvironment());

    @Test
    void shouldMapLlmExceptionsToDesignedHttpStatuses() {
        assertThat(statusOf(new InvalidAiResultException("invalid"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusOf(new NoLedgerPermissionException("forbidden"))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(statusOf(new LlmParseException("bad json"))).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(statusOf(new LlmClientException("unavailable"))).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private HttpStatus statusOf(Throwable throwable) {
        return invokeMethod(translator, "getMappedStatus", throwable);
    }
}
