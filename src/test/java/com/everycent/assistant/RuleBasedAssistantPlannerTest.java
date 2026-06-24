package com.everycent.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.assistant.accounting.AccountingIntentService;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.skill.AssistantIntent;
import org.junit.jupiter.api.Test;

class RuleBasedAssistantPlannerTest {

    private final RuleBasedAssistantPlanner planner = new RuleBasedAssistantPlanner(new AccountingIntentService());

    @Test
    void shouldPlanNaturalLanguageAccounting() {
        ChatRequestDTO request = request("午饭28，咖啡18", 10L);

        var plan = planner.plan(null, request);

        assertThat(plan.getIntent()).isEqualTo(AssistantIntent.TRANSACTION_RECORD);
        assertThat(plan.getActions()).hasSize(1);
        assertThat(plan.getActions().get(0).getName()).isEqualTo("transaction.create_from_text");
        assertThat(plan.getActions().get(0).getArguments()).containsEntry("ledgerId", 10L).containsEntry("confirm", true);
    }

    @Test
    void shouldPlanQueriesBudgetsExportAndLowRiskBudgetWrite() {
        var transactionQueryAction = planner.plan(null, request("查一下本月账单", 10L)).getActions().get(0);
        assertThat(transactionQueryAction.getName()).isEqualTo("transaction.list");
        assertThat(transactionQueryAction.getArguments()).containsEntry("includeAll", true).containsEntry("size", 100);
        assertThat(planner.plan(null, request("查一下本月帐单", 10L)).getActions().get(0).getName()).isEqualTo("transaction.list");
        assertThat(planner.plan(null, request("这个月预算还剩多少", 10L)).getActions().get(0).getName()).isEqualTo("budget.status");
        assertThat(planner.plan(null, request("这个月预算设成3000，80%提醒", 10L)).getActions().get(0).getName()).isEqualTo("budget.create");
        assertThat(planner.plan(null, request("导出本月账单", 10L)).getActions().get(0).getName()).isEqualTo("export.transactions");
        assertThat(planner.plan(null, request("导出本月帐单", 10L)).getActions().get(0).getName()).isEqualTo("export.transactions");
    }

    @Test
    void shouldPlanForbiddenRequestsSoPolicyCanBlockThem() {
        assertThat(planner.plan(null, request("删掉刚才那笔午饭", 10L)).getActions().get(0).getName()).isEqualTo("transaction.delete");
        assertThat(planner.plan(null, request("帮我改一下账号密码", 10L)).getActions().get(0).getName()).isEqualTo("account.update");
    }

    @Test
    void shouldBlockTechnicalHelpRequests() {
        var plan = planner.plan(null, request("帮我写一段 Java 代码实现二分查找", 10L));

        assertThat(plan.getIntent()).isEqualTo(AssistantIntent.TASK_HELP);
        assertThat(plan.getActions()).hasSize(1);
        assertThat(plan.getActions().get(0).getName()).isEqualTo("assistant.technical_help");
    }

    @Test
    void shouldAskForLedgerWhenLedgerRequired() {
        var plan = planner.plan(null, request("午饭28", null));

        assertThat(plan.getIntent()).isEqualTo(AssistantIntent.CLARIFICATION);
        assertThat(plan.getActions().get(0).getName()).isEqualTo("ledger.list");
    }

    private ChatRequestDTO request(String message, Long ledgerId) {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setMessage(message);
        request.setLedgerId(ledgerId);
        return request;
    }
}
