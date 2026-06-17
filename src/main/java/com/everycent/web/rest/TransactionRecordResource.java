package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import com.everycent.repository.UserRepository;
import com.everycent.security.SecurityUtils;
import com.everycent.service.LlmParsingService;
import com.everycent.service.TransactionRecordService;
import com.everycent.service.dto.TransactionPageDTO;
import com.everycent.service.dto.TransactionQueryDTO;
import com.everycent.service.dto.TransactionRecordDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class TransactionRecordResource {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionRecordResource.class);

    private static final String ENTITY_NAME = "transactionRecord";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final TransactionRecordService transactionRecordService;

    private final UserRepository userRepository;

    private final LlmParsingService llmParsingService;

    public TransactionRecordResource(
        TransactionRecordService transactionRecordService,
        UserRepository userRepository,
        LlmParsingService llmParsingService
    ) {
        this.transactionRecordService = transactionRecordService;
        this.userRepository = userRepository;
        this.llmParsingService = llmParsingService;
    }

    @GetMapping({ "/ledgers/{ledgerId}/transactions", "/ledgers/{ledgerId}/transactions/" })
    public ResponseEntity<TransactionPageDTO> getTransactions(
        @PathVariable Long ledgerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) TransactionType type,
        @RequestParam(required = false) String behaviorTagId,
        @RequestParam(required = false) String emotionTagId
    ) {
        User currentUser = getCurrentUser();
        TransactionQueryDTO queryDTO = new TransactionQueryDTO();
        queryDTO.setPage(page);
        queryDTO.setSize(size);
        queryDTO.setStartDate(startDate);
        queryDTO.setEndDate(endDate);
        queryDTO.setType(type);
        queryDTO.setBehaviorTagId(parseOptionalLong(behaviorTagId, "behaviorTagId"));
        queryDTO.setEmotionTagId(parseOptionalLong(emotionTagId, "emotionTagId"));
        LOG.debug("REST request to get transactions for Ledger : {}", ledgerId);
        return ResponseEntity.ok(transactionRecordService.findByLedger(currentUser, ledgerId, queryDTO));
    }

    @PostMapping({ "/ledgers/{ledgerId}/transactions", "/ledgers/{ledgerId}/transactions/" })
    public ResponseEntity<TransactionRecordDTO> createTransaction(
        @PathVariable Long ledgerId,
        @Valid @RequestBody TransactionRecordDTO transactionDTO
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to create transaction for Ledger : {}", ledgerId);
        TransactionRecordDTO result = transactionRecordService.create(currentUser, ledgerId, transactionDTO);
        return ResponseEntity
            .created(URI.create("/api/transactions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PostMapping({ "/ledgers/{ledgerId}/transactions/natural-language", "/ledgers/{ledgerId}/transactions/natural-language/" })
    public ResponseEntity<NaturalLanguageTransactionCreateResultDTO> createFromNaturalLanguage(
        @PathVariable Long ledgerId,
        @Valid @RequestBody NaturalLanguageTransactionCreateRequestDTO request
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to create transaction from natural language for Ledger : {}", ledgerId);
        return ResponseEntity.ok(llmParsingService.parseAndCreateTransaction(ledgerId, request, currentUser));
    }

    @GetMapping({ "/transactions/{transactionId}", "/transactions/{transactionId}/" })
    public ResponseEntity<TransactionRecordDTO> getTransaction(@PathVariable String transactionId) {
        User currentUser = getCurrentUser();
        Long parsedTransactionId = parseRequiredLong(transactionId, "transactionId");
        LOG.debug("REST request to get TransactionRecord : {}", parsedTransactionId);
        return ResponseEntity.ok(transactionRecordService.findOne(currentUser, parsedTransactionId));
    }

    @PutMapping({ "/transactions/{transactionId}", "/transactions/{transactionId}/" })
    public ResponseEntity<TransactionRecordDTO> updateTransaction(
        @PathVariable String transactionId,
        @Valid @RequestBody TransactionRecordDTO transactionDTO
    ) {
        User currentUser = getCurrentUser();
        Long parsedTransactionId = parseRequiredLong(transactionId, "transactionId");
        LOG.debug("REST request to update TransactionRecord : {}", parsedTransactionId);
        TransactionRecordDTO result = transactionRecordService.update(currentUser, parsedTransactionId, transactionDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, parsedTransactionId.toString()))
            .body(result);
    }

    @DeleteMapping({ "/transactions/{transactionId}", "/transactions/{transactionId}/" })
    public ResponseEntity<Void> deleteTransaction(@PathVariable String transactionId) {
        User currentUser = getCurrentUser();
        Long parsedTransactionId = parseRequiredLong(transactionId, "transactionId");
        LOG.debug("REST request to delete TransactionRecord : {}", parsedTransactionId);
        transactionRecordService.delete(currentUser, parsedTransactionId);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, parsedTransactionId.toString()))
            .build();
    }

    private User getCurrentUser() {
        String login = SecurityUtils
            .getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("Current user login not found", ENTITY_NAME, "usernotfound"));
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }

    private Long parseOptionalLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BadRequestAlertException("Invalid " + fieldName, ENTITY_NAME, "invalid" + fieldName.toLowerCase());
        }
    }

    private Long parseRequiredLong(String value, String fieldName) {
        Long parsed = parseOptionalLong(value, fieldName);
        if (parsed == null) {
            throw new BadRequestAlertException("Missing " + fieldName, ENTITY_NAME, "missing" + fieldName.toLowerCase());
        }
        return parsed;
    }
}
