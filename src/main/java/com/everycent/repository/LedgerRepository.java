package com.everycent.repository;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    List<Ledger> findAllByCreator(User creator);
}
