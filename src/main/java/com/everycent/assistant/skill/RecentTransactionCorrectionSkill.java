package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.service.TransactionRecordService;
import com.everycent.service.dto.TransactionRecordDTO;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RecentTransactionCorrectionSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("transaction.correct_recent");
    private static final Pattern MARKED_AMOUNT_PATTERN = Pattern.compile(
        "(?:应该是|应为|改成|改为|修改为|修正为|变成|总共|一共)\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块|rmb|RMB|¥)?"
    );
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块|rmb|RMB|¥)?");

    private final TransactionRecordService transactionRecordService;
    private final SkillCurrentUserResolver currentUserResolver;

    public RecentTransactionCorrectionSkill(
        TransactionRecordService transactionRecordService,
        SkillCurrentUserResolver currentUserResolver
    ) {
        this.transactionRecordService = transactionRecordService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "recent-transaction-correction-skill";
    }

    @Override
    public String description() {
        return "Correct the amount of a recently created transaction by matching its description in the user's correction text.";
    }

    @Override
    public boolean supports(String actionName) {
        return ACTIONS.contains(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        User user = currentUserResolver.resolve(context);
        ActionArgumentReader args = new ActionArgumentReader(action);
        String text = args.stringValue("text");
        BigDecimal correctedAmount = correctedAmount(text);
        if (correctedAmount == null) {
            return SkillResult.failure(action.getName(), "CORRECTION_AMOUNT_NOT_FOUND", "我没找到要改成多少金额，先把新金额说清楚喵。");
        }

        List<TransactionRecordDTO> recentRecords = transactionRecordService.findRecentByLedger(user, args.longValue("ledgerId"), args.intValue("limit", 20));
        Optional<TransactionRecordDTO> matched = recentRecords
            .stream()
            .filter(record -> matchScore(record, text) > 0)
            .max(Comparator.comparingInt(record -> matchScore(record, text)));
        if (matched.isEmpty()) {
            return SkillResult.failure(action.getName(), "CORRECTION_TARGET_NOT_FOUND", "我没在最近的记账里找到能对应上的条目，先告诉我具体是哪一笔喵。");
        }

        TransactionRecordDTO target = matched.get();
        TransactionRecordDTO update = copyForUpdate(target);
        update.setAmount(correctedAmount);
        update.setRawInput(text);
        return SkillResult.success(action.getName(), transactionRecordService.update(user, target.getId(), update));
    }

    private BigDecimal correctedAmount(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher markedMatcher = MARKED_AMOUNT_PATTERN.matcher(text);
        if (markedMatcher.find()) {
            return new BigDecimal(markedMatcher.group(1));
        }
        BigDecimal lastAmount = null;
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        while (matcher.find()) {
            lastAmount = new BigDecimal(matcher.group(1));
        }
        return lastAmount;
    }

    private int matchScore(TransactionRecordDTO record, String text) {
        if (record == null || !StringUtils.hasText(text)) {
            return 0;
        }
        String normalizedText = normalize(text);
        String description = normalize(record.getDescription());
        if (!StringUtils.hasText(description)) {
            return 0;
        }
        if (normalizedText.contains(description)) {
            return 100 + description.length();
        }
        int commonLength = longestCommonSubstringLength(description, normalizedText);
        int threshold = description.length() <= 2 ? 2 : 3;
        if (commonLength >= threshold) {
            return 20 + commonLength;
        }
        String behaviorTagName = normalize(record.getBehaviorTagName());
        if (StringUtils.hasText(behaviorTagName) && normalizedText.contains(behaviorTagName)) {
            return 40 + behaviorTagName.length();
        }
        return 0;
    }

    private TransactionRecordDTO copyForUpdate(TransactionRecordDTO source) {
        TransactionRecordDTO dto = new TransactionRecordDTO();
        dto.setLedgerId(source.getLedgerId());
        dto.setAmount(source.getAmount());
        dto.setType(source.getType());
        dto.setBehaviorTagId(source.getBehaviorTagId());
        dto.setEmotionTagId(source.getEmotionTagId());
        dto.setTransactionDate(source.getTransactionDate());
        dto.setDescription(source.getDescription());
        dto.setSource(source.getSource());
        dto.setRawInput(source.getRawInput());
        return dto;
    }

    private String normalize(String value) {
        return Objects.toString(value, "").replaceAll("\\s+", "").trim();
    }

    private int longestCommonSubstringLength(String left, String right) {
        int best = 0;
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    best = Math.max(best, dp[i][j]);
                }
            }
        }
        return best;
    }
}
