package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionSchemaValidatorTest {

    private final ActionSchemaValidator validator = new ActionSchemaValidator();

    @Test
    void shouldAcceptKnownSkillActionShape() {
        AssistantAction action = new AssistantAction(
            "transaction.create",
            Map.of("ledgerId", 1L, "amount", "28.00", "type", "EXPENSE", "recordDate", "2026-06-18")
        );

        assertThatCode(() -> validator.validate(action)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptMultiSegmentSkillActionName() {
        AssistantAction action = new AssistantAction("ledger.member.add", Map.of("ledgerId", 1L, "userId", 2L, "permissionLevel", "READ_ONLY"));

        assertThatCode(() -> validator.validate(action)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUrlSqlJavaClassAndRepositoryNames() {
        assertThatThrownBy(() -> validator.validate(new AssistantAction("/api/ledgers"))).isInstanceOf(InvalidActionException.class).hasMessageContaining("URL");
        assertThatThrownBy(() -> validator.validate(new AssistantAction("sql.execute"))).isInstanceOf(InvalidActionException.class).hasMessageContaining("SQL");
        assertThatThrownBy(() -> validator.validate(new AssistantAction("java.lang.Runtime.exec")))
            .isInstanceOf(InvalidActionException.class)
            .hasMessageContaining("Skill 白名单命名格式");
        assertThatThrownBy(() -> validator.validate(new AssistantAction("ledgerRepository.delete")))
            .isInstanceOf(InvalidActionException.class)
            .hasMessageContaining("Repository");
    }

    @Test
    void shouldRejectMissingRequiredArguments() {
        AssistantAction action = new AssistantAction("transaction.create", Map.of("ledgerId", 1L, "type", "EXPENSE", "recordDate", "2026-06-18"));

        assertThatThrownBy(() -> validator.validate(action)).isInstanceOf(InvalidActionException.class).hasMessageContaining("缺少必要参数：amount");
    }

    @Test
    void shouldAllowForbiddenDeleteActionThroughSchemaSoPolicyCanBlockItExplicitly() {
        AssistantAction action = new AssistantAction("transaction.delete");
        action.setRequiresConfirmation(true);

        assertThatCode(() -> validator.validate(action)).doesNotThrowAnyException();
    }
}
