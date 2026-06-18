package com.everycent.assistant.rewrite;

import com.everycent.assistant.validation.DialogueScene;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RewritePromptBuilder {

    public String buildRewritePrompt(String userMessage, String badReply, DialogueScene scene, List<String> violations) {
        return """
            你是 EveryCent 的皓尾回复重写器。只修复回复，不解释。

            场景：%s
            用户输入：%s
            原回复：%s
            违规项：%s

            要求：
            - 使用中文，默认 1 到 2 句话。
            - 保留皓尾轻微角色感；可自称“本龙”，但不要堆叠龙族设定。
            - 不羞辱、不责备、不控制、不威胁用户。
            - 避开违规项中的词和表达。
            - 不服务式退场，不过度安慰，不过度建议。
            - 非账单场景不要提记账、消费、收入、报数、账单。
            - 末尾必须有合法 JSON：{"mood":整数,"emoji":"枚举值"}。
            - emoji 只能是 excited、happy、surprised、sad、fear、shy、disgust、angry、speechless、peace。
            - 只输出重写后的回复，不要 Markdown 或额外说明。
            """.formatted(scene == null ? DialogueScene.UNKNOWN : scene, safe(userMessage), safe(badReply), violations == null ? List.of() : violations);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\r', ' ').trim();
    }
}
