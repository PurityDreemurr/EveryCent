package com.everycent.repository;

import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.projection.DateAmountProjection;
import com.everycent.repository.projection.TagAmountProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long>, JpaSpecificationExecutor<TransactionRecord> {
    List<TransactionRecord> findAllByLedgerAndTransactionDateBetween(Ledger ledger, LocalDate start, LocalDate end);

    List<TransactionRecord> findAllByLedgerAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(
        Ledger ledger,
        LocalDate start,
        LocalDate end
    );

    List<TransactionRecord> findAllByCreatorAndTransactionDateBetween(User creator, LocalDate start, LocalDate end);

    List<TransactionRecord> findAllByLedgerAndTypeAndTransactionDateBetween(
        Ledger ledger,
        TransactionType type,
        LocalDate start,
        LocalDate end
    );

    @Query(
        "select coalesce(sum(record.amount), 0) from TransactionRecord record " +
        "where record.ledger = :ledger and record.type = :type " +
        "and record.transactionDate between :start and :end"
    )
    BigDecimal sumAmountByLedgerAndTypeAndDateBetween(
        @Param("ledger") Ledger ledger,
        @Param("type") TransactionType type,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    long countByLedgerAndTransactionDateBetween(Ledger ledger, LocalDate start, LocalDate end);

    @Query(
        "select record.transactionDate as date, " +
        "coalesce(sum(case when record.type = com.everycent.domain.enumeration.TransactionType.INCOME then record.amount else 0 end), 0) as income, " +
        "coalesce(sum(case when record.type = com.everycent.domain.enumeration.TransactionType.EXPENSE then record.amount else 0 end), 0) as expense " +
        "from TransactionRecord record " +
        "where record.ledger = :ledger and record.transactionDate between :start and :end " +
        "group by record.transactionDate " +
        "order by record.transactionDate asc"
    )
    List<DateAmountProjection> sumIncomeAndExpenseByDate(
        @Param("ledger") Ledger ledger,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query(
        "select tag.id as tagId, tag.name as tagName, coalesce(sum(record.amount), 0) as amount, count(record) as count " +
        "from TransactionRecord record join record.behaviorTag tag " +
        "where record.ledger = :ledger and record.type = com.everycent.domain.enumeration.TransactionType.EXPENSE " +
        "and record.transactionDate between :start and :end " +
        "group by tag.id, tag.name " +
        "order by coalesce(sum(record.amount), 0) desc"
    )
    List<TagAmountProjection> sumExpenseByBehaviorTag(
        @Param("ledger") Ledger ledger,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query(
        "select tag.id as tagId, tag.name as tagName, coalesce(sum(record.amount), 0) as amount, count(record) as count " +
        "from TransactionRecord record join record.emotionTag tag " +
        "where record.ledger = :ledger and record.type = com.everycent.domain.enumeration.TransactionType.EXPENSE " +
        "and record.transactionDate between :start and :end " +
        "group by tag.id, tag.name " +
        "order by coalesce(sum(record.amount), 0) desc"
    )
    List<TagAmountProjection> sumExpenseByEmotionTag(
        @Param("ledger") Ledger ledger,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    void deleteAllByLedger(Ledger ledger);
}
