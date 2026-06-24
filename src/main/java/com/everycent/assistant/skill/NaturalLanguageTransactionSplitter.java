package com.everycent.assistant.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NaturalLanguageTransactionSplitter {

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[，,、；;\\n]+");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(".*\\d+(\\.\\d+)?\\s*(元|块|rmb|RMB|¥)?.*");
    private static final Pattern AMOUNT_VALUE_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?\\s*(?:元|块|rmb|RMB|¥)?");
    private static final Pattern TEXT_PATTERN = Pattern.compile(".*[\\p{IsHan}A-Za-z].*");
    private static final Pattern CARRY_OBJECT_PATTERN = Pattern.compile(".*(买|吃|喝|点|打车|坐|乘|购物|消费).*");
    private static final Pattern TEMPORAL_PATTERN = Pattern.compile("(昨天晚上|今天晚上|前天晚上|昨天|前天|今天|今晚|明天)");
    private static final Pattern SPEND_WITHOUT_AMOUNT_PATTERN = Pattern.compile(".*(都花了|花了|花掉|付了|用了).*");

    public List<String> split(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String[] parts = SEPARATOR_PATTERN.split(text.trim());
        List<String> records = new ArrayList<>();
        String pendingObject = null;
        String currentTemporal = "";
        String lastAmount = "";
        for (String part : parts) {
            String candidate = clean(part);
            String temporal = extractTemporal(candidate);
            if (StringUtils.hasText(temporal)) {
                currentTemporal = temporal;
            }
            if (isRecordCandidate(candidate)) {
                String normalized = normalizeRecord(candidate, pendingObject);
                records.add(applyTemporal(normalized, currentTemporal));
                lastAmount = extractAmount(candidate);
                pendingObject = null;
                continue;
            }
            String object = extractObject(candidate);
            if (StringUtils.hasText(object) && StringUtils.hasText(lastAmount) && SPEND_WITHOUT_AMOUNT_PATTERN.matcher(candidate).matches()) {
                records.add(applyTemporal(object + "花了" + lastAmount, currentTemporal));
                pendingObject = null;
                continue;
            }
            if (StringUtils.hasText(object) && CARRY_OBJECT_PATTERN.matcher(candidate).matches()) {
                pendingObject = object;
            }
        }
        if (records.size() <= 1) {
            return List.of(text.trim());
        }
        return records;
    }

    private String normalizeRecord(String text, String pendingObject) {
        String amount = extractAmount(text);
        String object = extractObject(text);
        if (!StringUtils.hasText(object) && StringUtils.hasText(pendingObject)) {
            object = pendingObject;
        }
        if (StringUtils.hasText(object) && StringUtils.hasText(amount)) {
            return object + amount;
        }
        return text;
    }

    private String applyTemporal(String text, String temporal) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(temporal) || text.contains(temporal)) {
            return text;
        }
        return temporal + text;
    }

    private String extractAmount(String text) {
        Matcher matcher = AMOUNT_VALUE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group().replaceAll("\\s+", "");
    }

    private String extractObject(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String object = text;
        object = AMOUNT_VALUE_PATTERN.matcher(object).replaceAll("");
        object = object.replaceAll("^(然后又|然后|接着|顺便|还有|再加|另外|另|和|还|又)\\s*", "");
        object = object.replaceAll("^(我|我们|今天晚上|昨天晚上|前天晚上|今天|昨天|前天|今晚|晚上)\\s*", "");
        object = object.replaceAll("^(买了|买|吃了|吃|喝了|喝|点了|点|坐了|坐|乘了|乘|打车|消费了|消费)\\s*", "");
        object = object.replaceAll("\\s*(都花了|花了|花掉|付了|付款|支出|用了|消费了|消费)$", "");
        object = object.replaceAll("\\s*(都)$", "");
        object = object.replaceAll("\\s*(了)$", "");
        object = object.trim();
        if (!StringUtils.hasText(object) || !TEXT_PATTERN.matcher(object).matches() || isContextOnly(object)) {
            return "";
        }
        return object;
    }

    private boolean isContextOnly(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        return text.matches("^(去|去了|在|到).{0,12}$") || text.matches("^(夜市|商场|超市|市场|路上|今天|今晚|晚上)$");
    }

    private String extractTemporal(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        Matcher matcher = TEMPORAL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private boolean isRecordCandidate(String text) {
        return StringUtils.hasText(text) && AMOUNT_PATTERN.matcher(text).matches() && TEXT_PATTERN.matcher(text).matches();
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("^(还有|再加|另外|另|和)\\s*", "").replaceAll("\\s*(帮我)?(记账|记帐|记一下|入账|入帐|记录一下)$", "");
    }
}
