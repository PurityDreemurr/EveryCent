package com.everycent.assistant.rewrite;

import com.everycent.assistant.validation.DialogueScene;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RewritePromptBuilder {

    public String buildRewritePrompt(String userMessage, String badReply, DialogueScene scene, List<String> violations) {
        DialogueScene resolvedScene = scene == null ? DialogueScene.UNKNOWN : scene;
        return """
            你是 EveryCent 财务助手回复重写器。只修复回复，不解释。

            场景：%s
            用户输入：%s
            原回复：%s
            违规项：%s
            目标样例：%s

            要求：
            - 使用中文，默认 1 到 2 句话。
            - 保持功能型财务助手语气，简洁、稳定、礼貌。
            - 不羞辱、不责备、不控制、不威胁用户。
            - 避开违规项中的词和表达。
            - 不服务式退场，不过度安慰，不过度建议。
            - 非账单场景不要提记账、消费、收入、报数、账单。
            - 末尾必须有合法 JSON：{"mood":整数,"emoji":"枚举值"}。
            - emoji 只能是 excited、happy、surprised、sad、fear、shy、disgust、angry、speechless、peace。
            - 只输出重写后的回复，不要 Markdown 或额外说明。
            """.formatted(resolvedScene, safe(userMessage), safe(badReply), violations == null ? List.of() : violations, exampleFor(resolvedScene));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\r', ' ').trim();
    }

    private String exampleFor(DialogueScene scene) {
        return switch (scene) {
            case SELF_BLAME, EMOTION_HEAVY -> "别这么判自己。今天状态差，不等于你这个人差。 {\"mood\":55,\"emoji\":\"sad\"}";
            case LONELINESS -> "一个人待着会有点发空。先让环境里有点声音吧。 {\"mood\":50,\"emoji\":\"sad\"}";
            case FATIGUE -> "累就先别硬撑。今天先到这里也可以。 {\"mood\":48,\"emoji\":\"peace\"}";
            case FRUSTRATION -> "这事确实烦。先把最关键的一步处理掉。 {\"mood\":58,\"emoji\":\"speechless\"}";
            case JOKE -> "行，先当作玩笑处理。 {\"mood\":52,\"emoji\":\"shy\"}";
            case COLD_REPLY -> "好，先不追问。 {\"mood\":40,\"emoji\":\"speechless\"}";
            case ACCOUNTING -> "金额还不确定，先别记错。是多少元？ {\"mood\":42,\"emoji\":\"peace\"}";
            case ACHIEVEMENT_SHARE -> "这个确实不容易，做得不错。 {\"mood\":55,\"emoji\":\"happy\"}";
            default -> "好，那就先这样。 {\"mood\":40,\"emoji\":\"peace\"}";
        };
    }
}
