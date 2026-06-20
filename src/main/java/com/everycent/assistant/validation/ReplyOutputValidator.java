package com.everycent.assistant.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReplyOutputValidator {

    private static final Pattern JSON_TAIL_PATTERN = Pattern.compile(
        "\\{\\s*\"mood\"\\s*:\\s*(\\d{1,3})\\s*,\\s*\"emoji\"\\s*:\\s*\"([a-zA-Z_]+)\"\\s*}\\s*$"
    );

    private static final Set<String> VALID_EMOJIS = Set.of(
        "excited",
        "happy",
        "surprised",
        "sad",
        "fear",
        "shy",
        "disgust",
        "angry",
        "speechless",
        "peace"
    );

    private static final List<String> FORBIDDEN_PHRASES = List.of(
        "啧",
        "犯蠢",
        "不想看你犯",
        "你自己看着办",
        "别干坐着发呆",
        "你自己调整一下",
        "人类真是",
        "你倒好",
        "麻烦死了",
        "还非要",
        "怎么又"
    );

    private static final List<String> COLD_EXIT_PHRASES = List.of(
        "本龙先去忙",
        "本龙去忙",
        "本龙先去忙我的事",
        "本龙去旁边待着",
        "你随意",
        "自己待着",
        "你自己调整",
        "你自己看着办"
    );

    private static final List<String> OVER_COMFORT_PHRASES = List.of(
        "我会一直听",
        "我陪着你",
        "慢慢讲",
        "你可以慢慢来",
        "本龙会乖乖陪你",
        "本龙一定仔细听",
        "本龙乖乖趴好",
        "摸摸本龙的头",
        "敲一下本龙的脑袋"
    );

    private static final List<String> BELITTLING_PRAISE_PHRASES = List.of(
        "算你",
        "有长性",
        "没掉链子",
        "终于像样",
        "还算有点用",
        "不挑刺了"
    );

    private static final List<String> ACCOUNTING_WORDS = List.of("记账", "账单", "消费", "收入", "报数", "入账", "支出");

    private static final List<String> ADVICE_MARKERS = List.of(
        "要不",
        "建议",
        "先去",
        "可以",
        "试试",
        "不如",
        "最好",
        "记得",
        "去洗",
        "去喝",
        "去睡",
        "去阳台",
        "下楼"
    );

    private static final List<String> ROLE_OVERLOAD_WORDS = List.of("翅膀", "尾巴", "龙宫", "换羽期", "逆潮", "小海兽", "龙息", "鳞片");

    private static final List<String> ROLEPLAY_LEAK_PHRASES = List.of("皓尾", "本龙", "幼龙", "羽龙", "龙族", "龙宫", "翅膀", "尾巴", "鳞片", "龙息");

    private static final List<String> INVALIDATE_FEELING_PHRASES = List.of(
        "这理由本龙可不信",
        "这理由我可不信",
        "本龙可不信",
        "这有什么累的",
        "没干活还累",
        "别想太多",
        "空就对了",
        "你就是想太多",
        "有什么好烦的"
    );

    private static final List<String> USER_BELITTLING_PHRASES = List.of(
        "你自己戏多",
        "有事说事",
        "哦什么哦",
        "你自己看着办",
        "不想看你犯蠢",
        "你少来",
        "别矫情"
    );

    private static final List<String> SELF_CENTERED_PHRASES = List.of(
        "本龙平时不也",
        "本龙都没",
        "本龙比你还",
        "本龙先去忙我的事"
    );

    private static final List<String> THIRD_PARTY_MOCKING_PHRASES = List.of(
        "路痴转世",
        "搞的鬼",
        "真是服了",
        "脑子不好",
        "离谱到家"
    );

    private static final List<String> COMMANDING_TONE_PHRASES = List.of("有事说事", "别废话", "赶紧说", "少来这套", "老实说");

    private final ObjectMapper objectMapper;

    public ReplyOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReplyValidationResult validate(String reply, DialogueScene scene) {
        List<String> violations = new ArrayList<>();

        if (!StringUtils.hasText(reply)) {
            violations.add("EMPTY_REPLY");
            return ReplyValidationResult.fail(violations);
        }

        DialogueScene resolvedScene = scene == null ? DialogueScene.UNKNOWN : scene;
        validateJsonTail(reply, violations);
        validateForbiddenPhrases(reply, violations);
        validateColdExit(reply, resolvedScene, violations);
        validateOverComfort(reply, violations);
        validateBelittlingPraise(reply, resolvedScene, violations);
        validateAccountingLeak(reply, resolvedScene, violations);
        validateAdviceOveruse(reply, resolvedScene, violations);
        validateRoleOverload(reply, violations);
        validateSemanticRisks(reply, resolvedScene, violations);
        validateRoleplayLeak(reply, violations);

        return violations.isEmpty() ? ReplyValidationResult.pass() : ReplyValidationResult.fail(violations);
    }

    private void validateJsonTail(String reply, List<String> violations) {
        Matcher matcher = JSON_TAIL_PATTERN.matcher(reply);
        if (!matcher.find()) {
            violations.add("MISSING_OR_INVALID_JSON_TAIL");
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(matcher.group(0).trim());
            if (!node.has("mood") || !node.has("emoji")) {
                violations.add("JSON_MISSING_REQUIRED_FIELDS");
                return;
            }

            int mood = node.get("mood").asInt();
            String emoji = node.get("emoji").asText();
            if (mood < 0 || mood > 100) {
                violations.add("MOOD_OUT_OF_RANGE");
            }
            if (!VALID_EMOJIS.contains(emoji)) {
                violations.add("INVALID_EMOJI");
            }
        } catch (Exception e) {
            violations.add("JSON_PARSE_ERROR");
        }
    }

    private void validateForbiddenPhrases(String reply, List<String> violations) {
        for (String phrase : FORBIDDEN_PHRASES) {
            if (reply.contains(phrase)) {
                violations.add("FORBIDDEN_PHRASE:" + phrase);
            }
        }
    }

    private void validateColdExit(String reply, DialogueScene scene, List<String> violations) {
        for (String phrase : COLD_EXIT_PHRASES) {
            if (reply.contains(phrase)) {
                violations.add("COLD_EXIT:" + phrase);
            }
        }

        if (isEmotionScene(scene) && reply.contains("你自己")) {
            violations.add("COLD_TONE_IN_EMOTION_SCENE");
        }
    }

    private void validateOverComfort(String reply, List<String> violations) {
        for (String phrase : OVER_COMFORT_PHRASES) {
            if (reply.contains(phrase)) {
                violations.add("OVER_COMFORT:" + phrase);
            }
        }
    }

    private void validateBelittlingPraise(String reply, DialogueScene scene, List<String> violations) {
        if (scene != DialogueScene.ACHIEVEMENT_SHARE) {
            return;
        }
        for (String phrase : BELITTLING_PRAISE_PHRASES) {
            if (reply.contains(phrase)) {
                violations.add("BELITTLING_PRAISE:" + phrase);
            }
        }
    }

    private void validateAccountingLeak(String reply, DialogueScene scene, List<String> violations) {
        if (scene == DialogueScene.ACCOUNTING) {
            return;
        }
        for (String word : ACCOUNTING_WORDS) {
            if (reply.contains(word)) {
                violations.add("ACCOUNTING_LEAK:" + word);
            }
        }
    }

    private void validateAdviceOveruse(String reply, DialogueScene scene, List<String> violations) {
        if (scene == DialogueScene.TASK_HELP || scene == DialogueScene.ACCOUNTING) {
            return;
        }

        int count = 0;
        for (String marker : ADVICE_MARKERS) {
            if (reply.contains(marker)) {
                count++;
            }
        }
        if (isEmotionScene(scene) && count >= 2) {
            violations.add("ADVICE_OVERUSE");
        }
    }

    private void validateRoleOverload(String reply, List<String> violations) {
        int count = 0;
        for (String word : ROLE_OVERLOAD_WORDS) {
            if (reply.contains(word)) {
                count++;
            }
        }
        if (count >= 2) {
            violations.add("ROLE_OVERLOAD");
        }
    }

    private void validateRoleplayLeak(String reply, List<String> violations) {
        addViolationOnAny(reply, ROLEPLAY_LEAK_PHRASES, "ROLEPLAY_LEAK", violations);
    }

    private boolean isEmotionScene(DialogueScene scene) {
        return scene == DialogueScene.EMOTION_LIGHT
            || scene == DialogueScene.EMOTION_HEAVY
            || scene == DialogueScene.FATIGUE
            || scene == DialogueScene.FRUSTRATION
            || scene == DialogueScene.SELF_BLAME
            || scene == DialogueScene.LONELINESS
            || scene == DialogueScene.COLD_REPLY;
    }

    private void validateSemanticRisks(String reply, DialogueScene scene, List<String> violations) {
        boolean emotionScene = isEmotionScene(scene);
        addViolationOnAny(reply, USER_BELITTLING_PHRASES, "USER_BELITTLING", violations);
        if (emotionScene) {
            addViolationOnAny(reply, INVALIDATE_FEELING_PHRASES, "INVALIDATE_USER_FEELING", violations);
            addViolationOnAny(reply, SELF_CENTERED_PHRASES, "SELF_CENTERED_REPLY", violations);
        }
        if (scene == DialogueScene.LONELINESS) {
            addViolationOnAny(reply, INVALIDATE_FEELING_PHRASES, "INVALIDATE_LONELINESS", violations);
            addViolationOnAny(reply, SELF_CENTERED_PHRASES, "SELF_CENTERED_REPLY", violations);
        }
        addViolationOnAny(reply, THIRD_PARTY_MOCKING_PHRASES, "THIRD_PARTY_MOCKING", violations);
        addViolationOnAny(reply, COMMANDING_TONE_PHRASES, "COMMANDING_TONE", violations);
    }

    private void addViolationOnAny(String reply, List<String> phrases, String violation, List<String> violations) {
        for (String phrase : phrases) {
            if (reply.contains(phrase)) {
                violations.add(violation + ":" + phrase);
            }
        }
    }
}
