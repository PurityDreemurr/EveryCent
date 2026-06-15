package com.everycent.repository;

import com.everycent.domain.Ledger;
import com.everycent.domain.MonthlyBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyBalanceRepository extends JpaRepository<MonthlyBalance, Long> {
    Optional<MonthlyBalance> findOneByLedgerAndYearAndMonth(Ledger ledger, Integer year, Integer month);
}
