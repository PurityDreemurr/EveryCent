package com.everycent.service;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LedgerPermissionService {

    private static final String ENTITY_NAME = "ledgerPermission";

    private final LedgerRepository ledgerRepository;

    private final UserLedgerPermissionRepository permissionRepository;

    public LedgerPermissionService(LedgerRepository ledgerRepository, UserLedgerPermissionRepository permissionRepository) {
        this.ledgerRepository = ledgerRepository;
        this.permissionRepository = permissionRepository;
    }

    public Ledger getLedgerOrThrow(Long ledgerId) {
        return ledgerRepository
            .findById(ledgerId)
            .orElseThrow(() -> new BadRequestAlertException("Ledger not found", ENTITY_NAME, "ledgernotfound"));
    }

    public UserLedgerPermission getActivePermission(User user, Ledger ledger) {
        return permissionRepository
            .findOneByUserAndLedgerAndStatus(user, ledger, PermissionStatus.ACTIVE)
            .orElseThrow(() -> new AccessDeniedException("No permission for this ledger"));
    }

    public Optional<UserLedgerPermission> findActivePermission(User user, Ledger ledger) {
        return permissionRepository.findOneByUserAndLedgerAndStatus(user, ledger, PermissionStatus.ACTIVE);
    }

    public void checkReadPermission(User user, Long ledgerId) {
        Ledger ledger = getLedgerOrThrow(ledgerId);
        getActivePermission(user, ledger);
    }

    public void checkWritePermission(User user, Long ledgerId) {
        Ledger ledger = getLedgerOrThrow(ledgerId);
        UserLedgerPermission permission = getActivePermission(user, ledger);
        if (!canWrite(permission.getPermissionLevel())) {
            throw new AccessDeniedException("Write permission is required");
        }
    }

    public void checkOwner(User user, Long ledgerId) {
        Ledger ledger = getLedgerOrThrow(ledgerId);
        UserLedgerPermission permission = getActivePermission(user, ledger);
        if (permission.getPermissionLevel() != PermissionLevel.OWNER) {
            throw new AccessDeniedException("Owner permission is required");
        }
    }

    public boolean canWrite(PermissionLevel permissionLevel) {
        return permissionLevel == PermissionLevel.OWNER || permissionLevel == PermissionLevel.READ_WRITE;
    }
}
