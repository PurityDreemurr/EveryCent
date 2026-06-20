package com.everycent.repository;

import com.everycent.domain.AiConversation;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findOneByIdAndUserAndLedgerAndArchivedFalse(Long id, User user, Ledger ledger);

    Optional<AiConversation> findFirstByUserAndLedgerAndArchivedFalseOrderByLastMessageDateDescIdDesc(User user, Ledger ledger);

    List<AiConversation> findAllByUserAndLedgerAndArchivedFalse(User user, Ledger ledger);
}
