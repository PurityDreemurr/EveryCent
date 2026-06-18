package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class NotificationServiceTest {

    private NotificationMessageRepository notificationMessageRepository;

    private NotificationService service;

    private User user;

    @BeforeEach
    void setUp() {
        notificationMessageRepository = org.mockito.Mockito.mock(NotificationMessageRepository.class);
        service = new NotificationService(notificationMessageRepository);

        user = new User();
        user.setId(1L);
        user.setLogin("admin");
    }

    @Test
    void findForUserShouldReturnPagedNotifications() {
        NotificationMessage notification = notification();
        when(notificationMessageRepository.findAllByUserOrderByCreatedDateDesc(user, PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(java.util.List.of(notification), PageRequest.of(0, 20), 1));

        NotificationPageDTO result = service.findForUser(user, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getLedgerId()).isEqualTo(10L);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    void markAsReadShouldUpdateOwnNotificationOnly() {
        NotificationMessage notification = notification();
        notification.setRead(false);
        when(notificationMessageRepository.findOneByIdAndUser(100L, user)).thenReturn(Optional.of(notification));
        when(notificationMessageRepository.save(any(NotificationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationMessageDTO result = service.markAsRead(user, 100L);

        assertThat(result.getRead()).isTrue();
        verify(notificationMessageRepository).save(notification);
    }

    @Test
    void deleteShouldRejectForeignNotification() {
        when(notificationMessageRepository.findOneByIdAndUser(100L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(user, 100L)).isInstanceOf(BadRequestAlertException.class);
    }

    private NotificationMessage notification() {
        Ledger ledger = new Ledger();
        ledger.setId(10L);

        Budget budget = new Budget();
        budget.setId(20L);

        NotificationMessage notification = new NotificationMessage();
        notification.setId(100L);
        notification.setUser(user);
        notification.setLedger(ledger);
        notification.setBudget(budget);
        notification.setTitle("预算提醒");
        notification.setContent("本月预算接近上限");
        notification.setType(NotificationType.BUDGET_ALERT);
        notification.setLevel(NotificationLevel.WARNING);
        notification.setRead(false);
        notification.setCreatedDate(Instant.parse("2026-06-18T00:00:00Z"));
        return notification;
    }
}
