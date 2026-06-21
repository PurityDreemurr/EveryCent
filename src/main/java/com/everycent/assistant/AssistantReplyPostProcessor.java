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
            case JOKE -> "{\"mood\":52,\"emoji\":\"shy\"}";
            case COLD_REPLY -> "{\"mood\":40,\"emoji\":\"speechless\"}";
            case SELF_BLAME, LONELINESS, EMOTION_HEAVY -> "{\"mood\":50,\"emoji\":\"sad\"}";
            case FATIGUE, EMOTION_LIGHT, ACCOUNTING, DAILY_CHAT, TASK_HELP, UNKNOWN -> "{\"mood\":40,\"emoji\":\"peace\"}";
            case FRUSTRATION -> "{\"mood\":50,\"emoji\":\"speechless\"}";
        };
    }

    private String fallback(DialogueScene scene) {
        return switch (scene == null ? DialogueScene.UNKNOWN : scene) {
            case ACHIEVEMENT_SHARE -> "这个确实不容易，做得不错。 {\"mood\":55,\"emoji\":\"happy\"}";
            case JOKE -> "行，先当作玩笑处理。 {\"mood\":52,\"emoji\":\"shy\"}";
            case COLD_REPLY -> "好，先不追问。 {\"mood\":40,\"emoji\":\"speechless\"}";
            case SELF_BLAME -> "别这么判自己。今天状态差，不等于你这个人差。 {\"mood\":55,\"emoji\":\"sad\"}";
            case LONELINESS -> "一个人待着会有点发空。先让环境里有点声音吧。 {\"mood\":50,\"emoji\":\"sad\"}";
            case FATIGUE -> "累就先别硬撑。今天先到这里也可以。 {\"mood\":48,\"emoji\":\"peace\"}";
            case FRUSTRATION -> "这事确实烦。先把最关键的一步处理掉。 {\"mood\":58,\"emoji\":\"speechless\"}";
            case EMOTION_HEAVY, EMOTION_LIGHT -> "先别急着压自己。我看到了。 {\"mood\":45,\"emoji\":\"peace\"}";
            case ACCOUNTING -> "这条信息还不完整，先别记错。请确认金额。 {\"mood\":45,\"emoji\":\"peace\"}";
            default -> "好，那就先这样。 {\"mood\":40,\"emoji\":\"peace\"}";
        };
    }
}
