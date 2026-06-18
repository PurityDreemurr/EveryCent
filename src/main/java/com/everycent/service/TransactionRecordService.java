package com.everycent.service;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.RecordSource;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.service.dto.TransactionPageDTO;
import com.everycent.service.dto.TransactionQueryDTO;
import com.everycent.service.dto.TransactionRecordDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransactionRecordService {

    private static final String ENTITY_NAME = "transactionRecord";

    private final TransactionRecordRepository transactionRecordRepository;

    private final BehaviorTagRepository behaviorTagRepository;

    private final EmotionTagRepository emotionTagRepository;

    private final LedgerPermissionService ledgerPermissionService;

    private final MonthlyBalanceService monthlyBalanceService;

    private final BudgetAlertService budgetAlertService;

    public TransactionRecordService(
        TransactionRecordRepository transactionRecordRepository,
        BehaviorTagRepository behaviorTagRepository,
        EmotionTagRepository emotionTagRepository,
        LedgerPermissionService ledgerPermissionService,
        MonthlyBalanceService monthlyBalanceService,
        BudgetAlertService budgetAlertService
    ) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.behaviorTagRepository = behaviorTagRepository;
        this.emotionTagRepository = emotionTagRepository;
        this.ledgerPermissionService = ledgerPermissionService;
        this.monthlyBalanceService = monthlyBalanceService;
        this.budgetAlertService = budgetAlertService;
    }

    @Transactional(readOnly = true)
    public TransactionPageDTO findByLedger(User currentUser, Long ledgerId, TransactionQueryDTO queryDTO) {
        ledgerPermissionService.checkReadPermission(currentUser, ledgerId);
        validateQuery(queryDTO);

        Specification<TransactionRecord> specification = belongsToLedger(ledgerId)
            .and(hasType(queryDTO.getType()))
            .and(transactionDateGreaterThanOrEqualTo(queryDTO.getStartDate()))
            .and(transactionDateLessThanOrEqualTo(queryDTO.getEndDate()))
            .and(hasBehaviorTag(queryDTO.getBehaviorTagId()))
            .and(hasEmotionTag(queryDTO.getEmotionTagId()));

        PageRequest pageRequest = PageRequest.of(
            queryDTO.getPage(),
            queryDTO.getSize(),
            Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<TransactionRecordDTO> page = transactionRecordRepository.findAll(specification, pageRequest).map(this::toDTO);

        TransactionPageDTO result = new TransactionPageDTO();
        result.setContent(page.getContent());
        result.setTotalElements(page.getTotalElements());
        result.setPage(queryDTO.getPage());
        result.setSize(queryDTO.getSize());
        return result;
    }

    public TransactionRecordDTO create(User currentUser, Long ledgerId, TransactionRecordDTO transactionDTO) {
        ledgerPermissionService.checkWritePermission(currentUser, ledgerId);
        Ledger ledger = ledgerPermissionService.getLedgerOrThrow(ledgerId);
        Instant now = Instant.now();

        TransactionRecord transaction = new TransactionRecord();
        transaction.setLedger(ledger);
        transaction.setCreator(currentUser);
        applyEditableFields(transaction, transactionDTO);
        transaction.setSource(transactionDTO.getSource() == null ? RecordSource.MANUAL : transactionDTO.getSource());
        transaction.setCreatedDate(now);
        transaction.setLastModifiedDate(now);

        TransactionRecord saved = transactionRecordRepository.save(transaction);
        monthlyBalanceService.recalculate(saved.getLedger(), saved.getTransactionDate());
        budgetAlertService.checkBudgetAlerts(currentUser, saved.getLedger(), saved.getTransactionDate());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public TransactionRecordDTO findOne(User currentUser, Long transactionId) {
        TransactionRecord transaction = getTransactionOrThrow(transactionId);
        ledgerPermissionService.checkReadPermission(currentUser, transaction.getLedger().getId());
        return toDTO(transaction);
    }

    public TransactionRecordDTO update(User currentUser, Long transactionId, TransactionRecordDTO transactionDTO) {
        TransactionRecord transaction = getTransactionOrThrow(transactionId);
        ledgerPermissionService.checkWritePermission(currentUser, transaction.getLedger().getId());
        if (transactionDTO.getLedgerId() != null && !Objects.equals(transactionDTO.getLedgerId(), transaction.getLedger().getId())) {
            throw new BadRequestAlertException("Transaction ledger cannot be changed", ENTITY_NAME, "ledgercannotchange");
        }

        java.time.LocalDate originalDate = transaction.getTransactionDate();
        applyEditableFields(transaction, transactionDTO);
        transaction.setLastModifiedDate(Instant.now());

        TransactionRecord saved = transactionRecordRepository.save(transaction);
        monthlyBalanceService.recalculate(saved.getLedger(), originalDate);
        budgetAlertService.checkBudgetAlerts(currentUser, saved.getLedger(), originalDate);
        if (!Objects.equals(originalDate, saved.getTransactionDate())) {
            monthlyBalanceService.recalculate(saved.getLedger(), saved.getTransactionDate());
            budgetAlertService.checkBudgetAlerts(currentUser, saved.getLedger(), saved.getTransactionDate());
        }
        return toDTO(saved);
    }

    public void delete(User currentUser, Long transactionId) {
        TransactionRecord transaction = getTransactionOrThrow(transactionId);
        ledgerPermissionService.checkWritePermission(currentUser, transaction.getLedger().getId());
        Ledger ledger = transaction.getLedger();
        java.time.LocalDate transactionDate = transaction.getTransactionDate();
        transactionRecordRepository.delete(transaction);
        monthlyBalanceService.recalculate(ledger, transactionDate);
        budgetAlertService.checkBudgetAlerts(currentUser, ledger, transactionDate);
    }

    private TransactionRecord getTransactionOrThrow(Long transactionId) {
        return transactionRecordRepository
            .findById(transactionId)
            .orElseThrow(() -> new BadRequestAlertException("Transaction record not found", ENTITY_NAME, "transactionnotfound"));
    }

    private void applyEditableFields(TransactionRecord transaction, TransactionRecordDTO transactionDTO) {
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(transactionDTO.getType());
        transaction.setTransactionDate(transactionDTO.getTransactionDate());
        transaction.setDescription(transactionDTO.getDescription());
        transaction.setRawInput(transactionDTO.getRawInput());
        transaction.setBehaviorTag(resolveBehaviorTag(transactionDTO.getBehaviorTagId()));
        transaction.setEmotionTag(resolveEmotionTag(transactionDTO.getEmotionTagId()));
    }

    private BehaviorTag resolveBehaviorTag(Long behaviorTagId) {
        if (behaviorTagId == null) {
            return null;
        }
        return behaviorTagRepository
            .findById(behaviorTagId)
            .orElseThrow(() -> new BadRequestAlertException("Behavior tag not found", ENTITY_NAME, "behaviortagnotfound"));
    }

    private EmotionTag resolveEmotionTag(Long emotionTagId) {
        if (emotionTagId == null) {
            return null;
        }
        return emotionTagRepository
            .findById(emotionTagId)
            .orElseThrow(() -> new BadRequestAlertException("Emotion tag not found", ENTITY_NAME, "emotiontagnotfound"));
    }

    private void validateQuery(TransactionQueryDTO queryDTO) {
        if (queryDTO.getStartDate() != null && queryDTO.getEndDate() != null && queryDTO.getStartDate().isAfter(queryDTO.getEndDate())) {
            throw new BadRequestAlertException("Start date cannot be after end date", ENTITY_NAME, "invaliddaterange");
        }
    }

    private TransactionRecordDTO toDTO(TransactionRecord transaction) {
        TransactionRecordDTO dto = new TransactionRecordDTO();
        dto.setId(transaction.getId());
        dto.setLedgerId(transaction.getLedger().getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setDescription(transaction.getDescription());
        dto.setSource(transaction.getSource());
        dto.setRawInput(transaction.getRawInput());
        dto.setCreatedDate(transaction.getCreatedDate());
        dto.setLastModifiedDate(transaction.getLastModifiedDate());
        if (transaction.getCreator() != null) {
            dto.setCreatorId(transaction.getCreator().getId());
            dto.setCreatedBy(transaction.getCreator().getId());
            dto.setCreatorLogin(transaction.getCreator().getLogin());
        }
        if (transaction.getBehaviorTag() != null) {
            dto.setBehaviorTagId(transaction.getBehaviorTag().getId());
            dto.setBehaviorTagName(transaction.getBehaviorTag().getName());
        }
        if (transaction.getEmotionTag() != null) {
            dto.setEmotionTagId(transaction.getEmotionTag().getId());
            dto.setEmotionTagName(transaction.getEmotionTag().getName());
        }
        return dto;
    }

    private Specification<TransactionRecord> belongsToLedger(Long ledgerId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("ledger").get("id"), ledgerId);
    }

    private Specification<TransactionRecord> hasType(Object type) {
        return (root, query, criteriaBuilder) -> type == null ? null : criteriaBuilder.equal(root.get("type"), type);
    }

    private Specification<TransactionRecord> transactionDateGreaterThanOrEqualTo(java.time.LocalDate startDate) {
        return (root, query, criteriaBuilder) ->
            startDate == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
    }

    private Specification<TransactionRecord> transactionDateLessThanOrEqualTo(java.time.LocalDate endDate) {
        return (root, query, criteriaBuilder) ->
            endDate == null ? null : criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate);
    }

    private Specification<TransactionRecord> hasBehaviorTag(Long behaviorTagId) {
        return (root, query, criteriaBuilder) ->
            behaviorTagId == null ? null : criteriaBuilder.equal(root.get("behaviorTag").get("id"), behaviorTagId);
    }

    private Specification<TransactionRecord> hasEmotionTag(Long emotionTagId) {
        return (root, query, criteriaBuilder) ->
            emotionTagId == null ? null : criteriaBuilder.equal(root.get("emotionTag").get("id"), emotionTagId);
    }
}
