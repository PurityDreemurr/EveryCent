package com.everycent.service;

import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.NotificationMessage;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import com.everycent.repository.NotificationMessageRepository;
import com.everycent.service.dto.NotificationMessageDTO;
import com.everycent.service.dto.NotificationPageDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final String ENTITY_NAME = "notification";

    private final NotificationMessageRepository notificationMessageRepository;

    public NotificationService(NotificationMessageRepository notificationMessageRepository) {
        this.notificationMessageRepository = notificationMessageRepository;
    }

    @Transactional(readOnly = true)
    public NotificationPageDTO findForUser(User currentUser, Boolean read, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size));
        Page<NotificationMessage> result =
            read == null
                ? notificationMessageRepository.findAllByUserOrderByCreatedDateDesc(currentUser, pageable)
                : notificationMessageRepository.findAllByUserAndReadOrderByCreatedDateDesc(currentUser, read, pageable);

        NotificationPageDTO dto = new NotificationPageDTO();
        dto.setContent(result.getContent().stream().map(this::toDTO).toList());
        dto.setTotalElements(result.getTotalElements());
        dto.setPage(result.getNumber());
        dto.setSize(result.getSize());
        return dto;
    }

    public NotificationMessageDTO markAsRead(User currentUser, Long notificationId) {
        NotificationMessage notification = getOwnNotificationOrThrow(currentUser, notificationId);
        notification.setRead(true);
        return toDTO(notificationMessageRepository.save(notification));
    }

    public void delete(User currentUser, Long notificationId) {
        NotificationMessage notification = getOwnNotificationOrThrow(currentUser, notificationId);
        notificationMessageRepository.delete(notification);
    }

    public NotificationMessageDTO create(
        User user,
        Ledger ledger,
        Budget budget,
        String title,
        String content,
        NotificationType type,
        NotificationLevel level
    ) {
        NotificationMessage notification = new NotificationMessage();
        notification.setUser(user);
        notification.setLedger(ledger);
        notification.setBudget(budget);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setLevel(level);
        notification.setRead(false);
        notification.setCreatedDate(Instant.now());
        return toDTO(notificationMessageRepository.save(notification));
    }

    private NotificationMessage getOwnNotificationOrThrow(User currentUser, Long notificationId) {
        return notificationMessageRepository
            .findOneByIdAndUser(notificationId, currentUser)
            .orElseThrow(() -> new BadRequestAlertException("Notification not found", ENTITY_NAME, "notificationnotfound"));
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private NotificationMessageDTO toDTO(NotificationMessage notification) {
        NotificationMessageDTO dto = new NotificationMessageDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUser() == null ? null : notification.getUser().getId());
        dto.setLedgerId(notification.getLedger() == null ? null : notification.getLedger().getId());
        dto.setBudgetId(notification.getBudget() == null ? null : notification.getBudget().getId());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setType(notification.getType());
        dto.setLevel(notification.getLevel());
        dto.setRead(notification.getRead());
        dto.setCreatedDate(notification.getCreatedDate());
        return dto;
    }
}
