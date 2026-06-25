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
    void shouldFailOnAccountingPushInDailyChat() {
        ReplyValidationResult result = validator.validate("今天要是想起什么消费或收入，直接报数就行。{\"mood\":40,\"emoji\":\"calm\"}", DialogueScene.DAILY_CHAT);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getViolations()).contains("ACCOUNTING_PUSH:直接报数");
    }

    @Test
    void shouldAllowNormalComfortingDailyChat() {
        ReplyValidationResult result = validator.validate("可以。先把爪子收回来，让这口气慢一点，今天不用马上把自己整理好喵。{\"mood\":45,\"emoji\":\"calm\"}", DialogueScene.DAILY_CHAT);
        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void shouldFailOnFormulaicCompanionComfort() {
        ReplyValidationResult result = validator.validate("别担心，我一直在这里陪你喵。{\"mood\":45,\"emoji\":\"calm\"}", DialogueScene.EMOTION_LIGHT);
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getViolations()).contains("OVER_COMFORT:我一直在这里");
    }

    @Test
    void shouldFailOnSemanticRiskPhrases() {
        assertThat(validator.validate("没干活还累？这理由我可不信。{\"mood\":40,\"emoji\":\"tired\"}", DialogueScene.FATIGUE).getViolations())
            .anyMatch(violation -> violation.startsWith("INVALIDATE_USER_FEELING"));
        assertThat(validator.validate("不是装成熟，是你自己戏多。{\"mood\":50,\"emoji\":\"pleased\"}", DialogueScene.JOKE).getViolations())
            .contains("USER_BELITTLING:你自己戏多");
        assertThat(validator.validate("哦什么哦。我看着呢，有事说事。{\"mood\":45,\"emoji\":\"calm\"}", DialogueScene.COLD_REPLY).getViolations())
            .contains("USER_BELITTLING:哦什么哦", "COMMANDING_TONE:有事说事");
        assertThat(validator.validate("空就对了，我平时不也一个人待着？{\"mood\":42,\"emoji\":\"sad\"}", DialogueScene.LONELINESS).getViolations())
            .anyMatch(violation -> violation.startsWith("INVALIDATE"));
        assertThat(validator.validate("这外卖小哥是路痴转世吗。{\"mood\":48,\"emoji\":\"tired\"}", DialogueScene.FRUSTRATION).getViolations())
            .contains("THIRD_PARTY_MOCKING:路痴转世");
    }

    @Test
    void shouldAllowOldRoleWordsWhenOtherwiseValid() {
        ReplyValidationResult result = validator.validate("本龙先按这条记下来了。{\"mood\":45,\"emoji\":\"calm\"}", DialogueScene.ACCOUNTING);
        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void shouldClassifyScenesSimply() {
        assertThat(classifier.classify("我今天花了28买午饭")).isEqualTo(DialogueScene.ACCOUNTING);
        assertThat(classifier.classify("外卖又送错了，真的无语")).isEqualTo(DialogueScene.FRUSTRATION);
        assertThat(classifier.classify("外卖花了 28，结果还送错了")).isEqualTo(DialogueScene.ACCOUNTING);
        assertThat(classifier.classify("帮我记一下外卖 28")).isEqualTo(DialogueScene.ACCOUNTING);
        assertThat(classifier.classify("午饭28，咖啡18")).isEqualTo(DialogueScene.ACCOUNTING);
        assertThat(classifier.classify("今天午饭挺难吃")).isEqualTo(DialogueScene.DAILY_CHAT);
        assertThat(classifier.classify("你不能安慰我一下吗")).isEqualTo(DialogueScene.EMOTION_LIGHT);
        assertThat(classifier.classify("我做完了")).isEqualTo(DialogueScene.ACHIEVEMENT_SHARE);
        assertThat(classifier.classify("嗯")).isEqualTo(DialogueScene.COLD_REPLY);
        assertThat(classifier.classify("改成非递归实现")).isEqualTo(DialogueScene.TASK_HELP);
        assertThat(classifier.classify("改成c'p'p")).isEqualTo(DialogueScene.TASK_HELP);
    }
}
