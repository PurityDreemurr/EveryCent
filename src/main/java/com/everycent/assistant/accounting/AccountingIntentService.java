package com.everycent.assistant.accounting;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccountingIntentService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?i)(\\d+(\\.\\d+)?\\s*(元|块|rmb|¥)?|[一二两三四五六七八九十百千万]+[块元])"
    );
    private static final Pattern EXPLICIT_ACCOUNTING_PATTERN = Pattern.compile(
        ".*(记账|记帐|帮我记|帮忙记|记一下|记一笔|记录一下|入账|入帐|记到账|记到帐|记到.+[账帐]本).*"
    );
    private static final Pattern FINANCE_ACTION_PATTERN = Pattern.compile(
        ".*(花了|花掉|付了|付款|买了|消费|支出|收入|工资|到账|到帐|退款|报销|转账|转帐|收了).*"
    );
    private static final Pattern SHORT_TRANSACTION_ITEM_PATTERN = Pattern.compile(
        ".*[\\p{IsHan}A-Za-z]{1,12}\\s*\\d+(\\.\\d+)?\\s*(元|块|rmb|RMB|¥)?.*"
    );

    public boolean isAccountingIntent(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return false;
        }
        String text = normalize(userMessage);
        if (EXPLICIT_ACCOUNTING_PATTERN.matcher(text).matches()) {
            return true;
        }
        boolean hasAmount = AMOUNT_PATTERN.matcher(text).find();
        if (!hasAmount) {
            return false;
        }
        return FINANCE_ACTION_PATTERN.matcher(text).matches() || isShortTransactionText(text);
    }

    private boolean isShortTransactionText(String text) {
        if (!SHORT_TRANSACTION_ITEM_PATTERN.matcher(text).matches()) {
            return false;
        }
        String compact = text.replaceAll("\\s+", "");
        return compact.matches(".*[，,、；;]\\p{IsHan}{1,12}\\d+.*") || compact.matches("^[\\p{IsHan}A-Za-z]{1,12}\\d+(\\.\\d+)?(元|块|rmb|RMB|¥)?$");
    }

    private String normalize(String text) {
        return text.trim().replace('，', ',').replace('；', ';').replace('：', ':');
    }
}
