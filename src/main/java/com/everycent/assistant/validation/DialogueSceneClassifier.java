package com.everycent.assistant.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DialogueSceneClassifier {

    public DialogueScene classify(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return DialogueScene.UNKNOWN;
        }

        String text = userMessage.trim();

        if (matches(text, ".*(花了|花掉|买了|消费|支出|收入|工资|到账|报销|退款|转账|\\d+元|\\d+块).*")) {
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
        if (containsAny(text, "累", "烦", "空", "难受", "不想说", "焦虑", "自责", "孤独")) {
            return DialogueScene.EMOTION_LIGHT;
        }
        if (containsAny(text, "怎么办", "怎么处理", "帮我", "帮忙", "怎么做", "分析一下")) {
            return DialogueScene.TASK_HELP;
        }
        return DialogueScene.DAILY_CHAT;
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
