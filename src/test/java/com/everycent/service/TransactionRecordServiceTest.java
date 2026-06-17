package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.EmotionValence;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.service.dto.TransactionPageDTO;
import com.everycent.service.dto.TransactionQueryDTO;
import com.everycent.service.dto.TransactionRecordDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class TransactionRecordServiceTest {

    private TransactionRecordRepository transactionRecordRepository;

    private BehaviorTagRepository behaviorTagRepository;

    private EmotionTagRepository emotionTagRepository;

    private LedgerPermissionService ledgerPermissionService;

    private TransactionRecordService service;

    private User user;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        transactionRecordRepository = org.mockito.Mockito.mock(TransactionRecordRepository.class);
        behaviorTagRepository = org.mockito.Mockito.mock(BehaviorTagRepository.class);
        emotionTagRepository = org.mockito.Mockito.mock(EmotionTagRepository.class);
        ledgerPermissionService = org.mockito.Mockito.mock(LedgerPermissionService.class);
        service = new TransactionRecordService(
            transactionRecordRepository,
            behaviorTagRepository,
            emotionTagRepository,
            ledgerPermissionService
        );

        user = user(1L, "admin");
        ledger = ledger(10L);
    }

    @Test
    void createShouldSaveManualExpenseTransaction() {
        TransactionRecordDTO requestDTO = transactionDTO();

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        TransactionRecordDTO result = service.create(user, 10L, requestDTO);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getLedgerId()).isEqualTo(10L);
        assertThat(result.getCreatorId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
        verify(ledgerPermissionService).checkWritePermission(user, 10L);
    }

    @Test
    void createShouldAttachExistingTags() {
        TransactionRecordDTO requestDTO = transactionDTO();
        requestDTO.setBehaviorTagId(20L);
        requestDTO.setEmotionTagId(30L);

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(behaviorTagRepository.findById(20L)).thenReturn(Optional.of(behaviorTag(20L, "Food")));
        when(emotionTagRepository.findById(30L)).thenReturn(Optional.of(emotionTag(30L, "Happy")));
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionRecordDTO result = service.create(user, 10L, requestDTO);

        assertThat(result.getBehaviorTagId()).isEqualTo(20L);
        assertThat(result.getBehaviorTagName()).isEqualTo("Food");
        assertThat(result.getEmotionTagId()).isEqualTo(30L);
        assertThat(result.getEmotionTagName()).isEqualTo("Happy");
    }

    @Test
    void createShouldRejectMissingBehaviorTag() {
        TransactionRecordDTO requestDTO = transactionDTO();
        requestDTO.setBehaviorTagId(404L);

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(behaviorTagRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(user, 10L, requestDTO)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void findOneShouldCheckReadPermissionOnTransactionLedger() {
        TransactionRecord transaction = transaction(100L);
        when(transactionRecordRepository.findById(100L)).thenReturn(Optional.of(transaction));

        TransactionRecordDTO result = service.findOne(user, 100L);

        assertThat(result.getId()).isEqualTo(100L);
        verify(ledgerPermissionService).checkReadPermission(user, 10L);
    }

    @Test
    void findByLedgerShouldReturnPagedResponse() {
        TransactionQueryDTO queryDTO = new TransactionQueryDTO();
        queryDTO.setPage(0);
        queryDTO.setSize(20);

        when(transactionRecordRepository.findAll(org.mockito.ArgumentMatchers.<Specification<TransactionRecord>>any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(transaction(100L))));

        TransactionPageDTO result = service.findByLedger(user, 10L, queryDTO);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
        verify(ledgerPermissionService).checkReadPermission(user, 10L);
    }

    @Test
    void updateShouldRejectLedgerChange() {
        TransactionRecord transaction = transaction(100L);
        TransactionRecordDTO requestDTO = transactionDTO();
        requestDTO.setLedgerId(99L);

        when(transactionRecordRepository.findById(100L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.update(user, 100L, requestDTO)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void deleteShouldCheckWritePermissionBeforeDeleting() {
        TransactionRecord transaction = transaction(100L);
        when(transactionRecordRepository.findById(100L)).thenReturn(Optional.of(transaction));

        service.delete(user, 100L);

        verify(ledgerPermissionService).checkWritePermission(user, 10L);
        verify(transactionRecordRepository).delete(transaction);
    }

    private TransactionRecordDTO transactionDTO() {
        TransactionRecordDTO dto = new TransactionRecordDTO();
        dto.setAmount(new BigDecimal("25.50"));
        dto.setType(TransactionType.EXPENSE);
        dto.setTransactionDate(LocalDate.of(2026, 6, 17));
        dto.setDescription("Lunch");
        return dto;
    }

    private TransactionRecord transaction(Long id) {
        TransactionRecord transaction = new TransactionRecord();
        transaction.setId(id);
        transaction.setLedger(ledger);
        transaction.setCreator(user);
        transaction.setAmount(new BigDecimal("25.50"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 17));
        transaction.setDescription("Lunch");
        transaction.setCreatedDate(Instant.now());
        return transaction;
    }

    private User user(Long id, String login) {
        User user = new User();
        user.setId(id);
        user.setLogin(login);
        return user;
    }

    private Ledger ledger(Long id) {
        Ledger ledger = new Ledger();
        ledger.setId(id);
        ledger.setName("Test ledger");
        return ledger;
    }

    private BehaviorTag behaviorTag(Long id, String name) {
        BehaviorTag tag = new BehaviorTag();
        tag.setId(id);
        tag.setCode("behavior-" + id);
        tag.setName(name);
        tag.setSystemDefault(true);
        return tag;
    }

    private EmotionTag emotionTag(Long id, String name) {
        EmotionTag tag = new EmotionTag();
        tag.setId(id);
        tag.setCode("emotion-" + id);
        tag.setName(name);
        tag.setValence(EmotionValence.POSITIVE);
        tag.setSystemDefault(true);
        return tag;
    }
}
