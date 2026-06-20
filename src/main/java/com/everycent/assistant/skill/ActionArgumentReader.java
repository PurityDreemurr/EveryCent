package com.everycent.assistant.skill;

import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.RecordSource;
import com.everycent.domain.enumeration.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ActionArgumentReader {

    private final Map<String, Object> arguments;

    public ActionArgumentReader(AssistantAction action) {
        this.arguments = action == null ? Map.of() : action.getArguments();
    }

    public Long longValue(String name) {
        Object value = arguments.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        throw new InvalidActionException("参数 " + name + " 必须是数字");
    }

    public int intValue(String name, int defaultValue) {
        Object value = arguments.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        throw new InvalidActionException("参数 " + name + " 必须是整数");
    }

    public Boolean booleanValue(String name) {
        Object value = arguments.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.valueOf(text);
        }
        throw new InvalidActionException("参数 " + name + " 必须是布尔值");
    }

    public String stringValue(String name) {
        Object value = arguments.get(name);
        return value == null ? null : value.toString();
    }

    public BigDecimal decimalValue(String name) {
        Object value = arguments.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number || value instanceof String) {
            String text = value.toString();
            return text.isBlank() ? null : new BigDecimal(text);
        }
        throw new InvalidActionException("参数 " + name + " 必须是数字");
    }

    public LocalDate dateValue(String name) {
        String value = stringValue(name);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    public TransactionType transactionType(String name) {
        String value = stringValue(name);
        return value == null || value.isBlank() ? null : TransactionType.valueOf(value);
    }

    public BudgetCycle budgetCycle(String name) {
        String value = stringValue(name);
        return value == null || value.isBlank() ? null : BudgetCycle.valueOf(value);
    }

    public RecordSource recordSource(String name) {
        String value = stringValue(name);
        return value == null || value.isBlank() ? null : RecordSource.valueOf(value);
    }
}
