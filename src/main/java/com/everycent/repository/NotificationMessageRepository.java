package com.everycent.repository;

import com.everycent.domain.NotificationMessage;
import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    List<NotificationMessage> findAllByUserAndReadOrderByCreatedDateDesc(User user, Boolean read);

    void deleteAllByLedger(Ledger ledger);

    void deleteAllByBudget(Budget budget);
}
