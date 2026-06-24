package com.everycent.service;

import com.everycent.domain.QqBotBinding;
import com.everycent.domain.User;
import com.everycent.repository.QqBotBindingRepository;
import com.everycent.service.dto.QqBotBindingDTO;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class QqBotBindingService {

    private static final String CODE_PREFIX = "ECQQ-";
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final QqBotBindingRepository qqBotBindingRepository;

    public QqBotBindingService(QqBotBindingRepository qqBotBindingRepository) {
        this.qqBotBindingRepository = qqBotBindingRepository;
    }

    public QqBotBindingDTO getOrCreateBinding(User user) {
        QqBotBinding binding = qqBotBindingRepository.findOneByUser(user).orElseGet(() -> createBinding(user));
        return toDTO(binding);
    }

    public QqBotBindingDTO resetBinding(User user) {
        qqBotBindingRepository
            .findOneByUser(user)
            .ifPresent(binding -> {
                qqBotBindingRepository.delete(binding);
                qqBotBindingRepository.flush();
            });
        return toDTO(createBinding(user));
    }

    public BindResult bind(String bindingCode, String qqOpenId) {
        if (!StringUtils.hasText(qqOpenId)) {
            return BindResult.failure("无法识别当前 QQ openid，暂时不能绑定喵。");
        }
        String normalizedCode = normalizeCode(bindingCode);
        QqBotBinding target = qqBotBindingRepository.findOneByBindingCode(normalizedCode).orElse(null);
        if (target == null) {
            return BindResult.failure("这个绑定码没找到喵，请到前端账户设置里复制最新的机器人绑定码。");
        }

        Optional<QqBotBinding> existingQqBinding = qqBotBindingRepository.findOneByQqOpenId(qqOpenId);
        if (existingQqBinding.isPresent() && !existingQqBinding.orElseThrow().getUser().getId().equals(target.getUser().getId())) {
            return BindResult.failure("这个 QQ 已经绑定了另一个 EveryCent 账号喵，一个 QQ 只能绑定一个账号。");
        }
        if (StringUtils.hasText(target.getQqOpenId()) && !target.getQqOpenId().equals(qqOpenId)) {
            return BindResult.failure("这个 EveryCent 账号已经绑定了另一个 QQ 喵，一个账号只能绑定一个 QQ。");
        }

        target.setQqOpenId(qqOpenId);
        target.setBoundDate(Instant.now());
        qqBotBindingRepository.save(target);
        return BindResult.success("绑定成功喵，接下来我会使用 EveryCent 账号「" + target.getUser().getLogin() + "」处理记账和查账。");
    }

    @Transactional(readOnly = true)
    public Optional<User> findBoundUser(String qqOpenId) {
        if (!StringUtils.hasText(qqOpenId)) {
            return Optional.empty();
        }
        return qqBotBindingRepository.findOneByQqOpenId(qqOpenId).map(QqBotBinding::getUser);
    }

    private QqBotBinding createBinding(User user) {
        QqBotBinding binding = new QqBotBinding();
        binding.setUser(user);
        binding.setBindingCode(generateCode());
        binding.setCreatedDate(Instant.now());
        return qqBotBindingRepository.save(binding);
    }

    private String generateCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_PREFIX);
            for (int i = 0; i < 8; i++) {
                builder.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
            }
            code = builder.toString();
        } while (qqBotBindingRepository.existsByBindingCode(code));
        return code;
    }

    private String normalizeCode(String bindingCode) {
        return bindingCode == null ? "" : bindingCode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private QqBotBindingDTO toDTO(QqBotBinding binding) {
        QqBotBindingDTO dto = new QqBotBindingDTO();
        dto.setBindingCode(binding.getBindingCode());
        dto.setBound(StringUtils.hasText(binding.getQqOpenId()));
        dto.setQqOpenIdMask(mask(binding.getQqOpenId()));
        return dto;
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() <= 8) {
            return value.charAt(0) + "***" + value.charAt(value.length() - 1);
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    public record BindResult(boolean success, String message) {
        public static BindResult success(String message) {
            return new BindResult(true, message);
        }

        public static BindResult failure(String message) {
            return new BindResult(false, message);
        }
    }
}
