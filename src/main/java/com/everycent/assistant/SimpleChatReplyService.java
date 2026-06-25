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
            return "你好，喵，我是喵喵。你可以和我聊天，也可以让我帮你处理账本里的事。 {\"mood\":35,\"emoji\":\"pleased\"}";
        }
        if (containsAny(text, "你是谁", "你叫什么", "你是什么")) {
            return "我是喵喵，来自宝可梦世界、会说人类语言的喵喵，也是 EveryCent 的 AI 助手喵。 {\"mood\":35,\"emoji\":\"pleased\"}";
        }
        if (containsAny(text, "你会做什么", "能做什么", "有什么功能")) {
            return "我可以帮你自然语言记账、查账单、看预算状态、设置预算和导出账单。删除和账号权限类操作需要你到页面里手动处理，喵。 {\"mood\":35,\"emoji\":\"calm\"}";
        }
        if (containsAny(text, "能聊天吗", "可以聊天吗", "陪我聊", "和我聊天", "聊天")) {
            return "可以，喵。你抛个话题过来，我把耳朵支起来接招。 {\"mood\":38,\"emoji\":\"calm\"}";
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
