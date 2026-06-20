package com.everycent.assistant.validation;

import com.everycent.assistant.accounting.AccountingIntentService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DialogueSceneClassifier {

    private final AccountingIntentService accountingIntentService;

    public DialogueSceneClassifier() {
        this(new AccountingIntentService());
    }

    public DialogueSceneClassifier(AccountingIntentService accountingIntentService) {
        this.accountingIntentService = accountingIntentService;
    }

    public DialogueScene classify(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return DialogueScene.UNKNOWN;
        }

        String text = userMessage.trim();

        if (isAccounting(text)) {
            return DialogueScene.ACCOUNTING;
        }
        if (matches(text, "^(哦|嗯|行吧|随便|不知道|算了|不聊了)$")) {
            return DialogueScene.COLD_REPLY;
        }
        if (containsAny(text, "装成熟", "小龙", "逗你", "开玩笑", "哈哈", "笑死")) {
            return DialogueScene.JOKE;
        }
        if (containsAny(text, "做完", "完成", "搞定", "收尾", "进展", "好了", "通过了")) {
            return DialogueScene.ACHIEVEMENT_SHARE;
        }
        if (containsAny(text, "废物", "不想活", "崩溃", "撑不住", "完蛋了", "绝望")) {
            return DialogueScene.EMOTION_HEAVY;
        }
        if (containsAny(text, "废物", "没用", "自责", "我不行", "我真差", "都是我的错")) {
            return DialogueScene.SELF_BLAME;
        }
        if (containsAny(text, "孤独", "一个人", "没人", "空落落", "心里空", "发空")) {
            return DialogueScene.LONELINESS;
        }
        if (containsAny(text, "累", "疲惫", "困", "没力气", "加班")) {
            return DialogueScene.FATIGUE;
        }
        if (containsAny(text, "烦", "无语", "气死", "火大", "不爽", "糟心")) {
            return DialogueScene.FRUSTRATION;
        }
        if (containsAny(text, "难受", "不想说", "焦虑", "压力")) {
            return DialogueScene.EMOTION_LIGHT;
        }
        if (containsAny(text, "怎么办", "怎么处理", "帮我", "帮忙", "怎么做", "分析一下")) {
            return DialogueScene.TASK_HELP;
        }
        return DialogueScene.DAILY_CHAT;
    }

    private boolean isAccounting(String text) {
        return accountingIntentService.isAccountingIntent(text);
    }

    private boolean matches(String text, String regex) {
        return text.matches(regex);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
