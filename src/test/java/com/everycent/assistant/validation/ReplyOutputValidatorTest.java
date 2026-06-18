package com.everycent.assistant.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ReplyOutputValidatorTest {

    private final ReplyOutputValidator validator = new ReplyOutputValidator(new ObjectMapper());
    private final DialogueSceneClassifier classifier = new DialogueSceneClassifier();

    @Test
    void shouldFailWhenJsonTailMissing() {
        ReplyValidationResult result = validator.validate("这话本龙听着有点不舒服。你只是累了，别这么贬低自己。", DialogueScene.EMOTION_LIGHT);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getViolations()).contains("MISSING_OR_INVALID_JSON_TAIL");
    }

    @Test
    void shouldFailOnForbiddenPhrase() {
        ReplyValidationResult result = validator.validate("啧，真是倒霉透顶。{\"mood\":45,\"emoji\":\"sad\"}", DialogueScene.DAILY_CHAT);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getViolations()).contains("FORBIDDEN_PHRASE:啧");
    }

    @Test
    void shouldFailOnAccountingLeak() {
        ReplyValidationResult result = validator.validate("今天要是想起什么消费或收入，直接报数就行。{\"mood\":40,\"emoji\":\"peace\"}", DialogueScene.DAILY_CHAT);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getViolations()).contains("ACCOUNTING_LEAK:消费", "ACCOUNTING_LEAK:收入", "ACCOUNTING_LEAK:报数");
    }

    @Test
    void shouldClassifyScenesSimply() {
        assertThat(classifier.classify("我今天花了28买午饭")).isEqualTo(DialogueScene.ACCOUNTING);
        assertThat(classifier.classify("我做完了")).isEqualTo(DialogueScene.ACHIEVEMENT_SHARE);
        assertThat(classifier.classify("嗯")).isEqualTo(DialogueScene.COLD_REPLY);
    }
}
