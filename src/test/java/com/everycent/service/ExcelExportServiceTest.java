package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.RecordSource;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExcelExportServiceTest {

    private TransactionRecordRepository transactionRecordRepository;

    private LedgerPermissionService ledgerPermissionService;

    private ExcelExportService service;

    private User user;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        transactionRecordRepository = org.mockito.Mockito.mock(TransactionRecordRepository.class);
        ledgerPermissionService = org.mockito.Mockito.mock(LedgerPermissionService.class);
        service = new ExcelExportService(transactionRecordRepository, ledgerPermissionService);

        user = new User();
        user.setId(1L);
        user.setLogin("admin");

        ledger = new Ledger();
        ledger.setId(10L);
        ledger.setName("Test ledger");
    }

    @Test
    void exportTransactionsShouldCreateXlsxWorkbook() throws Exception {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(transactionRecordRepository.findAllByLedgerAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(ledger, start, end))
            .thenReturn(List.of(transaction()));

        byte[] result = service.exportTransactions(user, 10L, start, end);

        assertThat(result).isNotEmpty();
        verify(ledgerPermissionService).checkReadPermission(user, 10L);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var sheet = workbook.getSheet("transactions");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("交易日期");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("2026-06-18");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("EXPENSE");
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(28.50);
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Food");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("Happy");
        }
    }

    @Test
    void exportTransactionsShouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> service.exportTransactions(user, 10L, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)))
            .isInstanceOf(BadRequestAlertException.class);
    }

    private TransactionRecord transaction() {
        BehaviorTag behaviorTag = new BehaviorTag();
        behaviorTag.setId(1L);
        behaviorTag.setName("Food");

        EmotionTag emotionTag = new EmotionTag();
        emotionTag.setId(1L);
        emotionTag.setName("Happy");

        TransactionRecord transaction = new TransactionRecord();
        transaction.setId(100L);
        transaction.setLedger(ledger);
        transaction.setCreator(user);
        transaction.setAmount(new BigDecimal("28.50"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 18));
        transaction.setBehaviorTag(behaviorTag);
        transaction.setEmotionTag(emotionTag);
        transaction.setDescription("Lunch");
        transaction.setSource(RecordSource.MANUAL);
        transaction.setCreatedDate(Instant.parse("2026-06-18T00:00:00Z"));
        return transaction;
    }
}
