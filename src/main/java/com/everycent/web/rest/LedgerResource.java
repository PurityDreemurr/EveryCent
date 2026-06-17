package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import com.everycent.security.SecurityUtils;
import com.everycent.service.LedgerService;
import com.everycent.service.dto.*;
import com.everycent.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class LedgerResource {

    private static final Logger LOG = LoggerFactory.getLogger(LedgerResource.class);

    private static final String ENTITY_NAME = "ledger";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final LedgerService ledgerService;

    private final UserRepository userRepository;

    public LedgerResource(LedgerService ledgerService, UserRepository userRepository) {
        this.ledgerService = ledgerService;
        this.userRepository = userRepository;
    }

    @GetMapping({ "/ledgers", "/ledgers/" })
    public ResponseEntity<List<LedgerDTO>> getLedgers() {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get ledgers for current user");
        return ResponseEntity.ok(ledgerService.findLedgersForUser(currentUser));
    }

    @PostMapping({ "/ledgers", "/ledgers/" })
    public ResponseEntity<LedgerDTO> createLedger(@Valid @RequestBody LedgerCreateDTO createDTO) throws URISyntaxException {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to create Ledger : {}", createDTO.getName());
        LedgerDTO result = ledgerService.createLedger(currentUser, createDTO);
        return ResponseEntity
            .created(new URI("/api/ledgers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @GetMapping({ "/ledgers/{ledgerId}", "/ledgers/{ledgerId}/" })
    public ResponseEntity<LedgerDTO> getLedger(@PathVariable Long ledgerId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get Ledger : {}", ledgerId);
        return ResponseEntity.ok(ledgerService.findOne(currentUser, ledgerId));
    }

    @PutMapping({ "/ledgers/{ledgerId}", "/ledgers/{ledgerId}/" })
    public ResponseEntity<LedgerDTO> updateLedger(@PathVariable Long ledgerId, @Valid @RequestBody LedgerUpdateDTO updateDTO) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to update Ledger : {}", ledgerId);
        LedgerDTO result = ledgerService.updateLedger(currentUser, ledgerId, updateDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ledgerId.toString()))
            .body(result);
    }

    @DeleteMapping({ "/ledgers/{ledgerId}", "/ledgers/{ledgerId}/" })
    public ResponseEntity<Void> deleteLedger(@PathVariable Long ledgerId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to delete Ledger : {}", ledgerId);
        ledgerService.deleteLedger(currentUser, ledgerId);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, ledgerId.toString()))
            .build();
    }

    @GetMapping({ "/ledgers/{ledgerId}/members", "/ledgers/{ledgerId}/members/" })
    public ResponseEntity<List<LedgerMemberDTO>> getMembers(@PathVariable Long ledgerId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get members for Ledger : {}", ledgerId);
        return ResponseEntity.ok(ledgerService.findMembers(currentUser, ledgerId));
    }

    @PostMapping({ "/ledgers/{ledgerId}/members", "/ledgers/{ledgerId}/members/" })
    public ResponseEntity<LedgerMemberDTO> addMember(
        @PathVariable Long ledgerId,
        @Valid @RequestBody LedgerMemberRequestDTO requestDTO
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to add member {} to Ledger : {}", requestDTO.getUserId(), ledgerId);
        LedgerMemberDTO result = ledgerService.addMember(currentUser, ledgerId, requestDTO);
        return ResponseEntity
            .created(URI.create("/api/ledgers/" + ledgerId + "/members/" + result.getUserId()))
            .body(result);
    }

    @PutMapping({ "/ledgers/{ledgerId}/members/{userId}", "/ledgers/{ledgerId}/members/{userId}/" })
    public ResponseEntity<LedgerMemberDTO> updateMember(
        @PathVariable Long ledgerId,
        @PathVariable Long userId,
        @Valid @RequestBody LedgerMemberUpdateDTO updateDTO
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to update member {} in Ledger : {}", userId, ledgerId);
        return ResponseEntity.ok(ledgerService.updateMember(currentUser, ledgerId, userId, updateDTO));
    }

    @DeleteMapping({ "/ledgers/{ledgerId}/members/{userId}", "/ledgers/{ledgerId}/members/{userId}/" })
    public ResponseEntity<Void> removeMember(@PathVariable Long ledgerId, @PathVariable Long userId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to remove member {} from Ledger : {}", userId, ledgerId);
        ledgerService.removeMember(currentUser, ledgerId, userId);
        return ResponseEntity.noContent().build();
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
