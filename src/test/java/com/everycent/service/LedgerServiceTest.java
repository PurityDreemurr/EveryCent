package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import com.everycent.repository.UserRepository;
import com.everycent.service.dto.LedgerCreateDTO;
import com.everycent.service.dto.LedgerDTO;
import com.everycent.service.dto.LedgerMemberDTO;
import com.everycent.service.dto.LedgerMemberRequestDTO;
import com.everycent.service.dto.LedgerMemberUpdateDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LedgerServiceTest {

    private LedgerRepository ledgerRepository;

    private UserLedgerPermissionRepository permissionRepository;

    private UserRepository userRepository;

    private LedgerPermissionService permissionService;

    private LedgerService service;

    private User owner;

    private User member;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        ledgerRepository = org.mockito.Mockito.mock(LedgerRepository.class);
        permissionRepository = org.mockito.Mockito.mock(UserLedgerPermissionRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        permissionService = org.mockito.Mockito.mock(LedgerPermissionService.class);
        service = new LedgerService(ledgerRepository, permissionRepository, userRepository, permissionService);

        owner = user(1L, "admin", "admin@localhost");
        member = user(2L, "user", "user@localhost");
        ledger = ledger(10L, owner);
    }

    @Test
    void createLedgerShouldCreateOwnerPermission() {
        LedgerCreateDTO createDTO = new LedgerCreateDTO();
        createDTO.setName("My ledger");
        createDTO.setDescription("Daily bookkeeping");

        when(ledgerRepository.save(any(Ledger.class))).thenAnswer(invocation -> {
            Ledger saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(permissionRepository.save(any(UserLedgerPermission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerDTO result = service.createLedger(owner, createDTO);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("My ledger");
        assertThat(result.getPermissionLevel()).isEqualTo(PermissionLevel.OWNER);
        verify(permissionRepository).save(
            org.mockito.ArgumentMatchers.argThat(permission ->
                    permission.getUser() == owner &&
                    permission.getLedger().getId().equals(10L) &&
                    permission.getPermissionLevel() == PermissionLevel.OWNER &&
                    permission.getStatus() == PermissionStatus.ACTIVE
                )
        );
    }

    @Test
    void findLedgersForUserShouldMapActivePermission() {
        when(ledgerRepository.findAllActiveLedgersForUser(owner)).thenReturn(List.of(ledger));
        when(permissionService.getActivePermission(owner, ledger)).thenReturn(permission(owner, PermissionLevel.OWNER));

        List<LedgerDTO> result = service.findLedgersForUser(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getPermissionLevel()).isEqualTo(PermissionLevel.OWNER);
    }

    @Test
    void addMemberShouldRejectDuplicateActivePermission() {
        LedgerMemberRequestDTO requestDTO = new LedgerMemberRequestDTO();
        requestDTO.setUserId(2L);
        requestDTO.setPermissionLevel(PermissionLevel.READ_WRITE);

        when(permissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(permissionService.findActivePermission(member, ledger)).thenReturn(Optional.of(permission(member, PermissionLevel.READ_ONLY)));

        assertThatThrownBy(() -> service.addMember(owner, 10L, requestDTO)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void addMemberShouldCreateReadWritePermission() {
        LedgerMemberRequestDTO requestDTO = new LedgerMemberRequestDTO();
        requestDTO.setUserId(2L);
        requestDTO.setPermissionLevel(PermissionLevel.READ_WRITE);

        when(permissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(permissionService.findActivePermission(member, ledger)).thenReturn(Optional.empty());
        when(permissionRepository.save(any(UserLedgerPermission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerMemberDTO result = service.addMember(owner, 10L, requestDTO);

        assertThat(result.getLedgerId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getPermissionLevel()).isEqualTo(PermissionLevel.READ_WRITE);
        assertThat(result.getStatus()).isEqualTo(PermissionStatus.ACTIVE);
    }

    @Test
    void updateMemberShouldRejectOwnerPermissionChanges() {
        LedgerMemberUpdateDTO updateDTO = new LedgerMemberUpdateDTO();
        updateDTO.setPermissionLevel(PermissionLevel.READ_ONLY);

        when(permissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(permissionService.getActivePermission(owner, ledger)).thenReturn(permission(owner, PermissionLevel.OWNER));

        assertThatThrownBy(() -> service.updateMember(owner, 10L, 1L, updateDTO)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void removeMemberShouldMarkPermissionRevoked() {
        UserLedgerPermission existing = permission(member, PermissionLevel.READ_WRITE);

        when(permissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(permissionService.getActivePermission(member, ledger)).thenReturn(existing);

        service.removeMember(owner, 10L, 2L);

        assertThat(existing.getStatus()).isEqualTo(PermissionStatus.REVOKED);
        verify(permissionRepository).save(existing);
    }

    @Test
    void deleteLedgerShouldRemovePermissionsBeforeDeletingLedger() {
        when(permissionService.getLedgerOrThrow(10L)).thenReturn(ledger);

        service.deleteLedger(owner, 10L);

        verify(permissionRepository).deleteAllByLedger(ledger);
        verify(permissionRepository).flush();
        verify(ledgerRepository).delete(ledger);
    }

    private User user(Long id, String login, String email) {
        User user = new User();
        user.setId(id);
        user.setLogin(login);
        user.setEmail(email);
        user.setActivated(true);
        return user;
    }

    private Ledger ledger(Long id, User creator) {
        Ledger ledger = new Ledger();
        ledger.setId(id);
        ledger.setName("Test ledger");
        ledger.setDescription("Test description");
        ledger.setCreator(creator);
        ledger.setCreatedDate(Instant.now());
        ledger.setLastModifiedDate(Instant.now());
        return ledger;
    }

    private UserLedgerPermission permission(User user, PermissionLevel level) {
        UserLedgerPermission permission = new UserLedgerPermission();
        permission.setUser(user);
        permission.setLedger(ledger);
        permission.setPermissionLevel(level);
        permission.setStatus(PermissionStatus.ACTIVE);
        permission.setCreatedDate(Instant.now());
        return permission;
    }
}
