package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import com.everycent.security.SecurityUtils;
import com.everycent.service.ExcelExportService;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ExportResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExportResource.class);

    private static final String ENTITY_NAME = "export";

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final ExcelExportService excelExportService;

    private final UserRepository userRepository;

    public ExportResource(ExcelExportService excelExportService, UserRepository userRepository) {
        this.excelExportService = excelExportService;
        this.userRepository = userRepository;
    }

    @GetMapping({ "/ledgers/{ledgerId}/transactions/export", "/ledgers/{ledgerId}/transactions/export/" })
    public ResponseEntity<byte[]> exportTransactions(
        @PathVariable Long ledgerId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to export transactions for Ledger : {}", ledgerId);
        byte[] result = excelExportService.exportTransactions(currentUser, ledgerId, startDate, endDate);
        return ResponseEntity
            .ok()
            .contentType(XLSX_MEDIA_TYPE)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("everycent-transactions.xlsx").build().toString())
            .body(result);
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
