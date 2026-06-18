package com.everycent.repository;

import com.everycent.domain.NotificationMessage;
import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    List<NotificationMessage> findAllByUserAndReadOrderByCreatedDateDesc(User user, Boolean read);

    Page<NotificationMessage> findAllByUserOrderByCreatedDateDesc(User user, Pageable pageable);

    Page<NotificationMessage> findAllByUserAndReadOrderByCreatedDateDesc(User user, Boolean read, Pageable pageable);

    Optional<NotificationMessage> findOneByIdAndUser(Long id, User user);

    boolean existsByUserAndBudgetAndTypeAndLevelAndRead(
        User user,
        Budget budget,
        NotificationType type,
        NotificationLevel level,
        Boolean read
    );

    void deleteAllByLedger(Ledger ledger);

    void deleteAllByBudget(Budget budget);
}
