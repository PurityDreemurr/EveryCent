package com.everycent.web.rest;

import com.everycent.domain.User;
import com.everycent.repository.UserRepository;
import com.everycent.security.SecurityUtils;
import com.everycent.service.NotificationService;
import com.everycent.service.dto.NotificationMessageDTO;
import com.everycent.service.dto.NotificationPageDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class NotificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationResource.class);

    private static final String ENTITY_NAME = "notification";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NotificationService notificationService;

    private final UserRepository userRepository;

    public NotificationResource(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping({ "/notifications", "/notifications/" })
    public ResponseEntity<NotificationPageDTO> getNotifications(
        @RequestParam(required = false) Boolean read,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to get Notifications for current user");
        return ResponseEntity.ok(notificationService.findForUser(currentUser, read, page, size));
    }

    @PatchMapping({ "/notifications/{notificationId}/read", "/notifications/{notificationId}/read/" })
    public ResponseEntity<NotificationMessageDTO> markAsRead(@PathVariable Long notificationId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to mark Notification as read : {}", notificationId);
        NotificationMessageDTO result = notificationService.markAsRead(currentUser, notificationId);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notificationId.toString()))
            .body(result);
    }

    @DeleteMapping({ "/notifications/{notificationId}", "/notifications/{notificationId}/" })
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        User currentUser = getCurrentUser();
        LOG.debug("REST request to delete Notification : {}", notificationId);
        notificationService.delete(currentUser, notificationId);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, notificationId.toString()))
            .build();
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
