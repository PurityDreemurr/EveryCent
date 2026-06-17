package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import com.everycent.service.LlmParsingService;
import com.everycent.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledgers/{ledgerId}/transactions")
public class TransactionRecordResource {

    private final LlmParsingService llmParsingService;
    private final UserService userService;

    public TransactionRecordResource(LlmParsingService llmParsingService, UserService userService) {
        this.llmParsingService = llmParsingService;
        this.userService = userService;
    }

    @PostMapping("/natural-language")
    public ResponseEntity<NaturalLanguageTransactionCreateResultDTO> createFromNaturalLanguage(
        @PathVariable Long ledgerId,
        @Valid @RequestBody NaturalLanguageTransactionCreateRequestDTO request
    ) {
        return ResponseEntity.ok(llmParsingService.parseAndCreateTransaction(ledgerId, request, currentUser()));
    }

    private User currentUser() {
        return userService.getUserWithAuthorities().orElseThrow(() -> new IllegalStateException("Current user could not be found"));
    }
}
