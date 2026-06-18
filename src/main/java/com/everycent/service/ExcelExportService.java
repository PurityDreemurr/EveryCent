package com.everycent.service;

import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExcelExportService {

    private static final String ENTITY_NAME = "export";

    private static final String[] HEADERS = {
        "交易日期",
        "类型",
        "金额",
        "行为标签",
        "情绪标签",
        "描述",
        "创建人",
        "来源",
        "创建时间",
    };

    private final TransactionRecordRepository transactionRecordRepository;

    private final LedgerPermissionService ledgerPermissionService;

    public ExcelExportService(
        TransactionRecordRepository transactionRecordRepository,
        LedgerPermissionService ledgerPermissionService
    ) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.ledgerPermissionService = ledgerPermissionService;
    }

    public byte[] exportTransactions(User currentUser, Long ledgerId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        ledgerPermissionService.checkReadPermission(currentUser, ledgerId);
        Ledger ledger = ledgerPermissionService.getLedgerOrThrow(ledgerId);
        List<TransactionRecord> transactions = transactionRecordRepository.findAllByLedgerAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(
            ledger,
            startDate,
            endDate
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("transactions");
            CellStyle headerStyle = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < transactions.size(); i++) {
                writeTransactionRow(sheet.createRow(i + 1), transactions.get(i));
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new BadRequestAlertException("Failed to export transactions", ENTITY_NAME, "exportfailed");
        }
    }

    private void writeTransactionRow(Row row, TransactionRecord transaction) {
        row.createCell(0).setCellValue(transaction.getTransactionDate() == null ? "" : transaction.getTransactionDate().toString());
        row.createCell(1).setCellValue(transaction.getType() == null ? "" : transaction.getType().name());
        if (transaction.getAmount() != null) {
            row.createCell(2).setCellValue(transaction.getAmount().doubleValue());
        } else {
            row.createCell(2).setCellValue("");
        }
        row.createCell(3).setCellValue(transaction.getBehaviorTag() == null ? "" : transaction.getBehaviorTag().getName());
        row.createCell(4).setCellValue(transaction.getEmotionTag() == null ? "" : transaction.getEmotionTag().getName());
        row.createCell(5).setCellValue(transaction.getDescription() == null ? "" : transaction.getDescription());
        row.createCell(6).setCellValue(transaction.getCreator() == null ? "" : transaction.getCreator().getLogin());
        row.createCell(7).setCellValue(transaction.getSource() == null ? "" : transaction.getSource().name());
        row
            .createCell(8)
            .setCellValue(
                transaction.getCreatedDate() == null
                    ? ""
                    : transaction.getCreatedDate().atZone(ZoneId.systemDefault()).toLocalDateTime().toString()
            );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestAlertException("Export date range is required", ENTITY_NAME, "daterangerequired");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestAlertException("Start date cannot be after end date", ENTITY_NAME, "invaliddaterange");
        }
    }
}
