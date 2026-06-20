package com.everycent.assistant;

import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.skill.AssistantPlan;
import com.everycent.domain.User;

public interface AssistantPlanner {
    AssistantPlan plan(User currentUser, ChatRequestDTO request);
}
