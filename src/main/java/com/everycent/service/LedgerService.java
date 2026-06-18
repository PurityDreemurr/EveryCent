package com.everycent.service;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.MonthlyBalanceRepository;
import com.everycent.repository.NotificationMessageRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import com.everycent.repository.UserRepository;
import com.everycent.service.dto.*;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
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

    private final TransactionRecordRepository transactionRecordRepository;

    private final BudgetRepository budgetRepository;

    private final MonthlyBalanceRepository monthlyBalanceRepository;

    private final NotificationMessageRepository notificationMessageRepository;

    private final NotificationService notificationService;

    public LedgerService(
        LedgerRepository ledgerRepository,
        UserLedgerPermissionRepository permissionRepository,
        UserRepository userRepository,
        LedgerPermissionService permissionService,
        TransactionRecordRepository transactionRecordRepository,
        BudgetRepository budgetRepository,
        MonthlyBalanceRepository monthlyBalanceRepository,
        NotificationMessageRepository notificationMessageRepository,
        NotificationService notificationService
    ) {
        this.ledgerRepository = ledgerRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.transactionRecordRepository = transactionRecordRepository;
        this.budgetRepository = budgetRepository;
        this.monthlyBalanceRepository = monthlyBalanceRepository;
        this.notificationMessageRepository = notificationMessageRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<LedgerDTO> findLedgersForUser(User user) {
        return ledgerRepository.findAllActiveLedgersForUser(user).stream().map(ledger -> toLedgerDTO(ledger, user)).toList();
    }

    public LedgerDTO createLedger(User user, LedgerCreateDTO createDTO) {
        String ledgerName = normalizeLedgerName(createDTO.getName());
        rejectDuplicateLedgerName(ledgerName, null);

        Instant now = Instant.now();
        Ledger ledger = new Ledger()
            .name(ledgerName)
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
        String ledgerName = normalizeLedgerName(updateDTO.getName());
        rejectDuplicateLedgerName(ledgerName, ledgerId);
        ledger.setName(ledgerName);
        ledger.setDescription(updateDTO.getDescription());
        ledger.setLastModifiedDate(Instant.now());
        Ledger savedLedger = ledgerRepository.save(ledger);
        return toLedgerDTO(savedLedger, user);
    }

    public void deleteLedger(User user, Long ledgerId) {
        permissionService.checkOwner(user, ledgerId);
        Ledger ledger = permissionService.getLedgerOrThrow(ledgerId);
        // Clear dependent ledger data first; the database keeps strict foreign keys.
        notificationMessageRepository.deleteAllByLedger(ledger);
        transactionRecordRepository.deleteAllByLedger(ledger);
        monthlyBalanceRepository.deleteAllByLedger(ledger);
        budgetRepository.deleteAllByLedger(ledger);
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
            UserLedgerPermission savedPermission = permissionRepository.save(existingPermission);
            createShareInviteNotification(owner, member, ledger);
            return toMemberDTO(savedPermission);
        }

        UserLedgerPermission permission = new UserLedgerPermission()
            .user(member)
            .ledger(ledger)
            .invitedBy(owner)
            .permissionLevel(requestDTO.getPermissionLevel())
            .status(PermissionStatus.ACTIVE)
            .createdDate(Instant.now());
        UserLedgerPermission savedPermission = permissionRepository.save(permission);
        createShareInviteNotification(owner, member, ledger);
        return toMemberDTO(savedPermission);
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

    private void createShareInviteNotification(User owner, User member, Ledger ledger) {
        notificationService.create(
            member,
            ledger,
            null,
            "账本共享邀请",
            owner.getLogin() + " 邀请你加入账本「" + ledger.getName() + "」。",
            NotificationType.SHARE_INVITE,
            NotificationLevel.INFO
        );
    }

    private String normalizeLedgerName(String name) {
        return name == null ? null : name.trim();
    }

    private void rejectDuplicateLedgerName(String name, Long currentLedgerId) {
        ledgerRepository
            .findFirstByNameIgnoreCase(name)
            .filter(existing -> !Objects.equals(existing.getId(), currentLedgerId))
            .ifPresent(existing -> {
                throw new BadRequestAlertException("Ledger name already exists", ENTITY_NAME, "ledgernameexists");
            });
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
