package com.everycent.repository;

import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    List<TransactionRecord> findAllByLedgerAndTransactionDateBetween(Ledger ledger, LocalDate start, LocalDate end);

    List<TransactionRecord> findAllByCreatorAndTransactionDateBetween(User creator, LocalDate start, LocalDate end);
}
