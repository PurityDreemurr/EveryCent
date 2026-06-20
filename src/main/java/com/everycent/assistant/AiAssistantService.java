package com.everycent.assistant;

import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AiAssistantService {

    private final AssistantApplicationService assistantApplicationService;

    public AiAssistantService(AssistantApplicationService assistantApplicationService) {
        this.assistantApplicationService = assistantApplicationService;
    }

    public ChatResponseDTO chat(User currentUser, ChatRequestDTO request) {
        return assistantApplicationService.chat(currentUser, request);
    }
}
