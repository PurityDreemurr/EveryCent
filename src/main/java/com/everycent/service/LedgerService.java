package com.everycent.service;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import com.everycent.repository.UserRepository;
import com.everycent.service.dto.*;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LedgerService {

    private static final String ENTITY_NAME = "ledger";

    private final LedgerRepository ledgerRepository;

    private final UserLedgerPermissionRepository permissionRepository;

    private final UserRepository userRepository;

    private final LedgerPermissionService permissionService;

    public LedgerService(
        LedgerRepository ledgerRepository,
        UserLedgerPermissionRepository permissionRepository,
        UserRepository userRepository,
        LedgerPermissionService permissionService
    ) {
        this.ledgerRepository = ledgerRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<LedgerDTO> findLedgersForUser(User user) {
        return ledgerRepository.findAllActiveLedgersForUser(user).stream().map(ledger -> toLedgerDTO(ledger, user)).toList();
    }

    public LedgerDTO createLedger(User user, LedgerCreateDTO createDTO) {
        Instant now = Instant.now();
        Ledger ledger = new Ledger()
            .name(createDTO.getName())
            .description(createDTO.getDescription())
            .creator(user)
            .createdDate(now)
            .lastModifiedDate(now);
        Ledger savedLedger = ledgerRepository.save(ledger);

        // A ledger is usable only after its creator receives OWNER permission.
        UserLedgerPermission ownerPermission = new UserLedgerPermission()
            .user(user)
            .ledger(savedLedger)
            .permissionLevel(PermissionLevel.OWNER)
            .status(PermissionStatus.ACTIVE)
            .createdDate(now);
        permissionRepository.save(ownerPermission);

        return toLedgerDTO(savedLedger, ownerPermission);
    }

    @Transactional(readOnly = true)
    public LedgerDTO findOne(User user, Long ledgerId) {
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        UserLedgerPermission permission = permissionService.getActivePermission(user, ledger);
        return toLedgerDTO(ledger, permission);
    }

    public LedgerDTO updateLedger(User user, Long ledgerId, LedgerUpdateDTO updateDTO) {
        permissionService.checkOwner(user, ledgerId);
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        ledger.setName(updateDTO.getName());
        ledger.setDescription(updateDTO.getDescription());
        ledger.setLastModifiedDate(Instant.now());
        Ledger savedLedger = ledgerRepository.save(ledger);
        return toLedgerDTO(savedLedger, user);
    }

    public void deleteLedger(User user, Long ledgerId) {
        permissionService.checkOwner(user, ledgerId);
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        // Remove member links first; otherwise Hibernate/MySQL still sees rows pointing at the ledger.
        permissionRepository.deleteAllByLedger(ledger);
        permissionRepository.flush();
        ledgerRepository.delete(ledger);
    }

    @Transactional(readOnly = true)
    public List<LedgerMemberDTO> findMembers(User user, Long ledgerId) {
        permissionService.checkReadPermission(user, ledgerId);
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        return permissionRepository.findAllByLedgerAndStatus(ledger, PermissionStatus.ACTIVE).stream().map(this::toMemberDTO).toList();
    }

    public LedgerMemberDTO addMember(User owner, Long ledgerId, LedgerMemberRequestDTO requestDTO) {
        permissionService.checkOwner(owner, ledgerId);
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        User member = userRepository
            .findById(requestDTO.getUserId())
            .orElseThrow(() -> new BadRequestAlertException("User not found", ENTITY_NAME, "usernotfound"));
        if (requestDTO.getPermissionLevel() == PermissionLevel.OWNER) {
            throw new BadRequestAlertException("Cannot add another owner", ENTITY_NAME, "ownerisnotassignable");
        }
        UserLedgerPermission existingPermission = permissionRepository.findOneByUserAndLedger(member, ledger).orElse(null);
        if (existingPermission != null && existingPermission.getStatus() == PermissionStatus.ACTIVE) {
            throw new BadRequestAlertException("User already has permission for this ledger", ENTITY_NAME, "permissionexists");
        }
        if (existingPermission != null) {
            existingPermission.setInvitedBy(owner);
            existingPermission.setPermissionLevel(requestDTO.getPermissionLevel());
            existingPermission.setStatus(PermissionStatus.ACTIVE);
            existingPermission.setCreatedDate(Instant.now());
            return toMemberDTO(permissionRepository.save(existingPermission));
        }

        UserLedgerPermission permission = new UserLedgerPermission()
            .user(member)
            .ledger(ledger)
            .invitedBy(owner)
            .permissionLevel(requestDTO.getPermissionLevel())
            .status(PermissionStatus.ACTIVE)
            .createdDate(Instant.now());
        return toMemberDTO(permissionRepository.save(permission));
    }

    public LedgerMemberDTO updateMember(User owner, Long ledgerId, Long userId, LedgerMemberUpdateDTO updateDTO) {
        permissionService.checkOwner(owner, ledgerId);
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        User member = userRepository
            .findById(userId)
            .orElseThrow(() -> new BadRequestAlertException("User not found", ENTITY_NAME, "usernotfound"));
        UserLedgerPermission permission = permissionService.getActivePermission(member, ledger);
        if (permission.getPermissionLevel() == PermissionLevel.OWNER || updateDTO.getPermissionLevel() == PermissionLevel.OWNER) {
            throw new BadRequestAlertException("Owner permission cannot be changed here", ENTITY_NAME, "ownercannotchange");
        }

        permission.setPermissionLevel(updateDTO.getPermissionLevel());
        return toMemberDTO(permissionRepository.save(permission));
    }

    public void removeMember(User owner, Long ledgerId, Long userId) {
        permissionService.checkOwner(owner, ledgerId);
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        User member = userRepository
            .findById(userId)
            .orElseThrow(() -> new BadRequestAlertException("User not found", ENTITY_NAME, "usernotfound"));
        UserLedgerPermission permission = permissionService.getActivePermission(member, ledger);
        if (permission.getPermissionLevel() == PermissionLevel.OWNER) {
            throw new BadRequestAlertException("Owner cannot be removed from ledger", ENTITY_NAME, "ownercannotremove");
        }
        permission.setStatus(PermissionStatus.REVOKED);
        permissionRepository.save(permission);
    }

    private LedgerDTO toLedgerDTO(Ledger ledger, User user) {
        return toLedgerDTO(ledger, permissionService.getActivePermission(user, ledger));
    }

    private LedgerDTO toLedgerDTO(Ledger ledger, UserLedgerPermission permission) {
        LedgerDTO dto = new LedgerDTO();
        dto.setId(ledger.getId());
        dto.setName(ledger.getName());
        dto.setDescription(ledger.getDescription());
        dto.setDefaultCurrency(ledger.getDefaultCurrency());
        dto.setCurrentMonthBalance(ledger.getCurrentMonthBalance());
        dto.setCreatedDate(ledger.getCreatedDate());
        dto.setLastModifiedDate(ledger.getLastModifiedDate());
        dto.setPermissionLevel(permission.getPermissionLevel());
        if (ledger.getCreator() != null) {
            dto.setCreatorId(ledger.getCreator().getId());
            dto.setCreatorLogin(ledger.getCreator().getLogin());
        }
        return dto;
    }

    private LedgerMemberDTO toMemberDTO(UserLedgerPermission permission) {
        LedgerMemberDTO dto = new LedgerMemberDTO();
        User user = permission.getUser();
        dto.setLedgerId(permission.getLedger().getId());
        dto.setUserId(user.getId());
        dto.setLogin(user.getLogin());
        dto.setEmail(user.getEmail());
        dto.setPermissionLevel(permission.getPermissionLevel());
        dto.setStatus(permission.getStatus());
        dto.setCreatedDate(permission.getCreatedDate());
        return dto;
    }
}
