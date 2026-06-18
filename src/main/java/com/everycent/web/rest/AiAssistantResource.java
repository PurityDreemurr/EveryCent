package com.everycent.web.rest;

import com.everycent.assistant.AiAssistantService;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import com.everycent.security.SecurityUtils;
import com.everycent.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AiAssistantResource {

    private static final String ENTITY_NAME = "aiAssistant";

    private final AiAssistantService aiAssistantService;
    private final UserRepository userRepository;

    public AiAssistantResource(AiAssistantService aiAssistantService, UserRepository userRepository) {
        this.aiAssistantService = aiAssistantService;
        this.userRepository = userRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        return ResponseEntity.ok(aiAssistantService.chat(getCurrentUser(), request));
    }

    private User getCurrentUser() {
        String login = SecurityUtils
            .getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("Current user login not found", ENTITY_NAME, "usernotfound"));
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }
}
