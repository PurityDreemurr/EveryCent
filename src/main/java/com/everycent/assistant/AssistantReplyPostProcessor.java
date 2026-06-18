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

    private String fallback(DialogueScene scene) {
        return switch (scene == null ? DialogueScene.UNKNOWN : scene) {
            case ACHIEVEMENT_SHARE -> "这个确实不容易。该夸。 {\"mood\":55,\"emoji\":\"happy\"}";
            case JOKE -> "本龙才没装成熟，这叫偶尔靠谱。 {\"mood\":52,\"emoji\":\"shy\"}";
            case COLD_REPLY -> "行吧，本龙先不追问。 {\"mood\":40,\"emoji\":\"speechless\"}";
            case SELF_BLAME -> "别这么判自己。今天状态差，不等于你这个人差。 {\"mood\":55,\"emoji\":\"sad\"}";
            case LONELINESS -> "一个人待着是会突然发空。先让屋里有点声音吧。 {\"mood\":50,\"emoji\":\"sad\"}";
            case FATIGUE -> "累就先别硬撑。今天到这一步也算撑住了。 {\"mood\":48,\"emoji\":\"peace\"}";
            case FRUSTRATION -> "这事确实烦。本龙先站你这边。 {\"mood\":58,\"emoji\":\"speechless\"}";
            case EMOTION_HEAVY, EMOTION_LIGHT -> "先别急着压自己。本龙听见了。 {\"mood\":45,\"emoji\":\"peace\"}";
            case ACCOUNTING -> "这条本龙先没说准，等确认后再记。 {\"mood\":45,\"emoji\":\"peace\"}";
            default -> "行，那就先这样。 {\"mood\":40,\"emoji\":\"peace\"}";
        };
    }
}
