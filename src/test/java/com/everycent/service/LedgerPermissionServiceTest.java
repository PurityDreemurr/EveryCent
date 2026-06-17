package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class LedgerPermissionServiceTest {

    private LedgerRepository ledgerRepository;

    private UserLedgerPermissionRepository permissionRepository;

    private LedgerPermissionService service;

    private User user;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        ledgerRepository = org.mockito.Mockito.mock(LedgerRepository.class);
        permissionRepository = org.mockito.Mockito.mock(UserLedgerPermissionRepository.class);
        service = new LedgerPermissionService(ledgerRepository, permissionRepository);

        user = new User();
        user.setId(1L);
        user.setLogin("admin");

        ledger = new Ledger();
        ledger.setId(10L);
        ledger.setName("Test ledger");
    }

    @Test
    void checkReadPermissionShouldPassWhenActivePermissionExists() {
        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(permissionRepository.findOneByUserAndLedgerAndStatus(user, ledger, PermissionStatus.ACTIVE))
            .thenReturn(Optional.of(permission(PermissionLevel.READ_ONLY)));

        service.checkReadPermission(user, 10L);
    }

    @Test
    void checkReadPermissionShouldDenyWhenPermissionDoesNotExist() {
        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(permissionRepository.findOneByUserAndLedgerAndStatus(user, ledger, PermissionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkReadPermission(user, 10L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkWritePermissionShouldAllowOwnerAndReadWriteOnly() {
        assertThat(service.canWrite(PermissionLevel.OWNER)).isTrue();
        assertThat(service.canWrite(PermissionLevel.READ_WRITE)).isTrue();
        assertThat(service.canWrite(PermissionLevel.READ_ONLY)).isFalse();
    }

    @Test
    void checkWritePermissionShouldDenyReadOnlyUser() {
        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(permissionRepository.findOneByUserAndLedgerAndStatus(user, ledger, PermissionStatus.ACTIVE))
            .thenReturn(Optional.of(permission(PermissionLevel.READ_ONLY)));

        assertThatThrownBy(() -> service.checkWritePermission(user, 10L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkOwnerShouldDenyReadWriteUser() {
        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(permissionRepository.findOneByUserAndLedgerAndStatus(user, ledger, PermissionStatus.ACTIVE))
            .thenReturn(Optional.of(permission(PermissionLevel.READ_WRITE)));

        assertThatThrownBy(() -> service.checkOwner(user, 10L)).isInstanceOf(AccessDeniedException.class);
    }

    private UserLedgerPermission permission(PermissionLevel level) {
        UserLedgerPermission permission = new UserLedgerPermission();
        permission.setUser(user);
        permission.setLedger(ledger);
        permission.setPermissionLevel(level);
        permission.setStatus(PermissionStatus.ACTIVE);
        return permission;
    }
}
