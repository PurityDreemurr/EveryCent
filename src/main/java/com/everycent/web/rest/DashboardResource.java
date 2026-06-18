package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import com.everycent.security.SecurityUtils;
import com.everycent.service.DashboardService;
import com.everycent.service.dto.DashboardSummaryDTO;
import com.everycent.service.dto.TagStatDTO;
import com.everycent.service.dto.TrendPointDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardResource {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardResource.class);

    private static final String ENTITY_NAME = "dashboard";

    private final DashboardService dashboardService;

    private final UserRepository userRepository;

    public DashboardResource(DashboardService dashboardService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping({ "/ledgers/{ledgerId}/dashboard/summary", "/ledgers/{ledgerId}/dashboard/summary/" })
    public ResponseEntity<DashboardSummaryDTO> getSummary(
        @PathVariable Long ledgerId,
        @RequestParam(defaultValue = "MONTH") String period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get dashboard summary for Ledger : {}", ledgerId);
        return ResponseEntity.ok(dashboardService.getSummary(currentUser, ledgerId, period, date));
    }

    @GetMapping({ "/ledgers/{ledgerId}/dashboard/trend", "/ledgers/{ledgerId}/dashboard/trend/" })
    public ResponseEntity<List<TrendPointDTO>> getTrend(
        @PathVariable Long ledgerId,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get dashboard trend for Ledger : {}", ledgerId);
        return ResponseEntity.ok(dashboardService.getTrend(currentUser, ledgerId, parseOptionalDate(startDate, "startDate"), parseOptionalDate(endDate, "endDate")));
    }

    @GetMapping({ "/ledgers/{ledgerId}/dashboard/behavior-tags", "/ledgers/{ledgerId}/dashboard/behavior-tags/" })
    public ResponseEntity<List<TagStatDTO>> getBehaviorTagStats(
        @PathVariable Long ledgerId,
        @RequestParam(defaultValue = "MONTH") String period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get dashboard behavior tag stats for Ledger : {}", ledgerId);
        return ResponseEntity.ok(dashboardService.getBehaviorTagStats(currentUser, ledgerId, period, date));
    }

    @GetMapping({ "/ledgers/{ledgerId}/dashboard/emotion-tags", "/ledgers/{ledgerId}/dashboard/emotion-tags/" })
    public ResponseEntity<List<TagStatDTO>> getEmotionTagStats(
        @PathVariable Long ledgerId,
        @RequestParam(defaultValue = "MONTH") String period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get dashboard emotion tag stats for Ledger : {}", ledgerId);
        return ResponseEntity.ok(dashboardService.getEmotionTagStats(currentUser, ledgerId, period, date));
    }

    private User getCurrentUser() {
        String login = SecurityUtils
            .getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("Current user login not found", ENTITY_NAME, "usernotfound"));
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }

    private LocalDate parseOptionalDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException ex) {
            throw new BadRequestAlertException("Invalid " + fieldName, ENTITY_NAME, "invalid" + fieldName.toLowerCase());
        }
    }
}
