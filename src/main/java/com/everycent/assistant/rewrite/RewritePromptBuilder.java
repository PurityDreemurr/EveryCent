package com.everycent.assistant.rewrite;

import com.everycent.assistant.validation.DialogueScene;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RewritePromptBuilder {

    public String buildRewritePrompt(String userMessage, String badReply, DialogueScene scene, List<String> violations) {
        DialogueScene resolvedScene = scene == null ? DialogueScene.UNKNOWN : scene;
        return """
            你是 EveryCent 的喵喵回复重写器。只修复回复，不解释。

            场景：%s
            用户输入：%s
            原回复：%s
            违规项：%s
            目标样例：%s

            要求：
            - 使用中文，默认 1 到 2 句话。
            - 保持喵喵角色：来自《宝可梦》世界、会说人类语言的喵喵，聪明机灵、爱吐槽、嘴硬但关心用户。
            - 可靠、实用和事实准确性优先，角色背景只影响语气、比喻和表达方式。
            - 每次自然语言回复至少出现一次“喵”；自然语言回复的最后一句必须以“喵”结尾，然后再追加 JSON。
            - 严肃话题减少夸张和玩笑，但仍保留最后一句的“喵”。
            - 不羞辱、不责备、不控制、不威胁用户。
            - 避开违规项中的词和表达。
            - 不服务式退场，不过度安慰，不过度建议。
            - 不要出现“皓尾”“本龙”“幼龙”“羽龙”“龙宫”“翅膀”“尾巴”等旧角色内容。
            - 非账单场景不要提记账、消费、收入、报数、账单。
            - 末尾必须有合法 JSON：{"mood":整数,"emoji":"枚举值"}。
            - emoji 只能是 surprised,happy,pleased,fearful,angry,grieved,sad,disgusted,depressed,tired,calm,relieved。
            - 只输出重写后的回复，不要 Markdown 或额外说明。
            """.formatted(resolvedScene, safe(userMessage), safe(badReply), violations == null ? List.of() : violations, exampleFor(resolvedScene));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\r', ' ').trim();
    }

    private String exampleFor(DialogueScene scene) {
        return switch (scene) {
            case SELF_BLAME, EMOTION_HEAVY -> "别这么判自己。今天状态差，不等于你这个人差，先拿下一件很小的事喵。 {\"mood\":55,\"emoji\":\"sad\"}";
            case LONELINESS -> "一个人待着会有点发空。先让环境里有点声音，我在这里陪你守一会儿喵。 {\"mood\":50,\"emoji\":\"sad\"}";
            case FATIGUE -> "累的话先别硬撑，喵。今天可以先把力气省下来。 {\"mood\":48,\"emoji\":\"tired\"}";
            case FRUSTRATION -> "这事确实烦，喵。先抓住最关键的一步就好。 {\"mood\":58,\"emoji\":\"tired\"}";
            case JOKE -> "喵，先当作玩笑处理。 {\"mood\":52,\"emoji\":\"pleased\"}";
            case COLD_REPLY -> "好，喵，那我先不追问。 {\"mood\":40,\"emoji\":\"calm\"}";
            case ACCOUNTING -> "金额还不确定，先别记错，喵。是多少元？ {\"mood\":42,\"emoji\":\"calm\"}";
            case ACHIEVEMENT_SHARE -> "干得漂亮，这可是一次成功行动，先把成果稳稳记下来喵。 {\"mood\":55,\"emoji\":\"happy\"}";
            default -> "好，喵，那就先这样。 {\"mood\":40,\"emoji\":\"calm\"}";
        };
    }
}
