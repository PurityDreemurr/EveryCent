package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.llm.dto.AiAlertRequestDTO;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.TransactionParseRequestDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.service.LlmParsingService;
import com.everycent.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class LlmResource {

    private final LlmParsingService llmParsingService;
    private final UserService userService;

    public LlmResource(LlmParsingService llmParsingService, UserService userService) {
        this.llmParsingService = llmParsingService;
        this.userService = userService;
    }

    @PostMapping("/transaction/parse")
    public ResponseEntity<TransactionParseResultDTO> parseTransaction(@Valid @RequestBody TransactionParseRequestDTO request) {
        return ResponseEntity.ok(llmParsingService.parseTransaction(request, currentUser()));
    }

    @PostMapping("/budget-alert/generate")
    public ResponseEntity<AiAlertResultDTO> generateBudgetAlert(@Valid @RequestBody AiAlertRequestDTO request) {
        return ResponseEntity.ok(llmParsingService.generateBudgetAlert(request, currentUser()));
    }

    private User currentUser() {
        return userService.getUserWithAuthorities().orElseThrow(() -> new IllegalStateException("Current user could not be found"));
    }
}
