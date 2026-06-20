package com.everycent.assistant;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SimpleChatReplyService {

    public String reply(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return null;
        }
        String text = userMessage.trim();
        if (matchesAny(text, "你好", "您好", "hello", "hi")) {
            return "你好，我是 EveryCent 的 AI 助手。可以陪你聊，也可以帮你查账、记账和看预算。 {\"mood\":35,\"emoji\":\"peace\"}";
        }
        if (containsAny(text, "你是谁", "你叫什么", "你是什么")) {
            return "我是 EveryCent 的 AI 助手，主要帮你处理记账、查账、预算和导出，也能做一点普通聊天。 {\"mood\":35,\"emoji\":\"peace\"}";
        }
        if (containsAny(text, "你会做什么", "能做什么", "有什么功能")) {
            return "我可以帮你自然语言记账、查账单、看预算状态、设置预算和导出账单。删除和账号权限类操作需要你到页面里手动处理。 {\"mood\":35,\"emoji\":\"peace\"}";
        }
        if (containsAny(text, "能聊天吗", "可以聊天吗", "陪我聊", "和我聊天", "聊天")) {
            return "可以。你想轻松聊几句也行，想顺手记账或查预算也可以直接说。 {\"mood\":38,\"emoji\":\"peace\"}";
        }
        return null;
    }

    private boolean matchesAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
