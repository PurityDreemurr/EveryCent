package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.repository.UserRepository;
import com.everycent.security.SecurityUtils;
import com.everycent.service.BudgetService;
import com.everycent.service.dto.BudgetDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class BudgetResource {

    private static final Logger LOG = LoggerFactory.getLogger(BudgetResource.class);

    private static final String ENTITY_NAME = "budget";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BudgetService budgetService;

    private final UserRepository userRepository;

    public BudgetResource(BudgetService budgetService, UserRepository userRepository) {
        this.budgetService = budgetService;
        this.userRepository = userRepository;
    }

    @GetMapping({ "/ledgers/{ledgerId}/budgets", "/ledgers/{ledgerId}/budgets/" })
    public ResponseEntity<List<BudgetDTO>> getBudgets(@PathVariable Long ledgerId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get Budgets for Ledger : {}", ledgerId);
        return ResponseEntity.ok(budgetService.findByLedger(currentUser, ledgerId));
    }

    @PostMapping({ "/ledgers/{ledgerId}/budgets", "/ledgers/{ledgerId}/budgets/" })
    public ResponseEntity<BudgetDTO> createBudget(@PathVariable Long ledgerId, @Valid @RequestBody BudgetDTO budgetDTO) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to create Budget for Ledger : {}", ledgerId);
        BudgetDTO result = budgetService.create(currentUser, ledgerId, budgetDTO);
        return ResponseEntity
            .created(URI.create("/api/budgets/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping({ "/budgets/{budgetId}", "/budgets/{budgetId}/" })
    public ResponseEntity<BudgetDTO> updateBudget(@PathVariable Long budgetId, @Valid @RequestBody BudgetDTO budgetDTO) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to update Budget : {}", budgetId);
        BudgetDTO result = budgetService.update(currentUser, budgetId, budgetDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, budgetId.toString()))
            .body(result);
    }

    @DeleteMapping({ "/budgets/{budgetId}", "/budgets/{budgetId}/" })
    public ResponseEntity<Void> deleteBudget(@PathVariable Long budgetId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to delete Budget : {}", budgetId);
        budgetService.delete(currentUser, budgetId);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, budgetId.toString()))
            .build();
    }

    @GetMapping({ "/ledgers/{ledgerId}/budgets/status", "/ledgers/{ledgerId}/budgets/status/" })
    public ResponseEntity<BudgetDTO> getBudgetStatus(
        @PathVariable Long ledgerId,
        @RequestParam BudgetCycle cycle,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get Budget status for Ledger : {}", ledgerId);
        return ResponseEntity.ok(budgetService.getStatus(currentUser, ledgerId, cycle, date));
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
