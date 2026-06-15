package com.everycent.repository;

import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.enumeration.BudgetCycle;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByLedgerAndEnabledTrue(Ledger ledger);

    Optional<Budget> findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(
        Ledger ledger,
        BudgetCycle cycle,
        LocalDate periodStart,
        LocalDate periodEnd
    );
}
