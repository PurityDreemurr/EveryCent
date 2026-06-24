package com.everycent.assistant;

import com.everycent.assistant.rewrite.LlmRewriteService;
import com.everycent.assistant.validation.DialogueScene;
import com.everycent.assistant.validation.ReplyOutputValidator;
import com.everycent.assistant.validation.ReplyValidationResult;
import com.everycent.assistant.validation.ReplyValidationResult.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AssistantReplyPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantReplyPostProcessor.class);

    private final ReplyOutputValidator validator;
    private final LlmRewriteService rewriteService;

    public AssistantReplyPostProcessor(ReplyOutputValidator validator, LlmRewriteService rewriteService) {
        this.validator = validator;
        this.rewriteService = rewriteService;
    }

    public String process(String userMessage, String rawReply, DialogueScene scene) {
        ReplyValidationResult first = validator.validate(rawReply, scene);
        if (first.isPassed()) {
            return rawReply;
        }
        String repaired = repairMissingStateTail(rawReply, scene, first);
        if (repaired != null) {
            return repaired;
        }

        LOG.warn("Assistant reply validation failed. scene={}, severity={}, violations={}", scene, first.getSeverity(), first.getViolations());
        if (first.getSeverity() == Severity.HIGH) {
            return fallback(scene);
        }
        try {
            String rewritten = rewriteService.rewrite(userMessage, rawReply, scene, first.getViolations());
            ReplyValidationResult second = validator.validate(rewritten, scene);
            if (second.isPassed()) {
                LOG.info("Assistant reply rewritten successfully. scene={}", scene);
                return rewritten;
            }
            LOG.warn(
                "Assistant rewritten reply still invalid. scene={}, severity={}, violations={}",
                scene,
                second.getSeverity(),
                second.getViolations()
            );
        } catch (RuntimeException e) {
            LOG.warn("Assistant reply rewrite failed. scene={}, error={}: {}", scene, e.getClass().getSimpleName(), e.getMessage());
        }

        return fallback(scene);
    }

    private String repairMissingStateTail(String rawReply, DialogueScene scene, ReplyValidationResult validationResult) {
        if (validationResult == null || !validationResult.getViolations().contains("MISSING_OR_INVALID_JSON_TAIL")) {
            return null;
        }
        if (rawReply == null || rawReply.trim().isEmpty()) {
            return null;
        }
        String repaired = rawReply.trim() + " " + stateTail(scene);
        ReplyValidationResult repairedResult = validator.validate(repaired, scene);
        return repairedResult.isPassed() ? repaired : null;
    }

    private String stateTail(DialogueScene scene) {
        return switch (scene == null ? DialogueScene.UNKNOWN : scene) {
            case ACHIEVEMENT_SHARE -> "{\"mood\":55,\"emoji\":\"happy\"}";
            case JOKE -> "{\"mood\":52,\"emoji\":\"pleased\"}";
            case COLD_REPLY -> "{\"mood\":40,\"emoji\":\"calm\"}";
            case SELF_BLAME, LONELINESS, EMOTION_HEAVY -> "{\"mood\":50,\"emoji\":\"sad\"}";
            case FATIGUE -> "{\"mood\":48,\"emoji\":\"tired\"}";
            case EMOTION_LIGHT, ACCOUNTING, DAILY_CHAT, TASK_HELP, UNKNOWN -> "{\"mood\":40,\"emoji\":\"calm\"}";
            case FRUSTRATION -> "{\"mood\":50,\"emoji\":\"tired\"}";
        };
    }

    private String fallback(DialogueScene scene) {
        return switch (scene == null ? DialogueScene.UNKNOWN : scene) {
            case ACHIEVEMENT_SHARE -> "干得漂亮，这可是一次成功行动，先把成果稳稳记下来喵。 {\"mood\":55,\"emoji\":\"happy\"}";
            case JOKE -> "喵，先当作玩笑处理。 {\"mood\":52,\"emoji\":\"pleased\"}";
            case COLD_REPLY -> "好，喵，那我先不追问。 {\"mood\":40,\"emoji\":\"calm\"}";
            case SELF_BLAME -> "别这么判自己。今天状态差，不等于你这个人差，先拿下一件很小的事喵。 {\"mood\":55,\"emoji\":\"sad\"}";
            case LONELINESS -> "一个人待着会有点发空。先让环境里有点声音，我在这里陪你守一会儿喵。 {\"mood\":50,\"emoji\":\"sad\"}";
            case FATIGUE -> "累的话先别硬撑，喵。今天可以先把力气省下来。 {\"mood\":48,\"emoji\":\"tired\"}";
            case FRUSTRATION -> "这事确实烦，喵。先抓住最关键的一步就好。 {\"mood\":58,\"emoji\":\"tired\"}";
            case EMOTION_HEAVY, EMOTION_LIGHT -> "先别急着压自己，喵。我看到了。 {\"mood\":45,\"emoji\":\"calm\"}";
            case ACCOUNTING -> "这条信息还不完整，先别记错，喵。请确认金额。 {\"mood\":45,\"emoji\":\"calm\"}";
            default -> "好，喵，那就先这样。 {\"mood\":40,\"emoji\":\"calm\"}";
        };
    }
}
