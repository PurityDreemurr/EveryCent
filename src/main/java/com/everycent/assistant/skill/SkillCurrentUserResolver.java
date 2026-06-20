package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class SkillCurrentUserResolver {

    private final UserRepository userRepository;

    public SkillCurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User resolve(SkillExecutionContext context) {
        if (context == null || context.getUserId() == null) {
            throw new InvalidActionException("SkillExecutionContext 缺少当前用户 ID");
        }
        return userRepository
            .findById(context.getUserId())
            .orElseThrow(() -> new InvalidActionException("当前用户不存在：" + context.getUserId()));
    }
}
