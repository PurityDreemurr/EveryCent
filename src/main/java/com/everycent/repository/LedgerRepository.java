package com.everycent.repository;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    List<Ledger> findAllByCreator(User creator);

    Optional<Ledger> findFirstByNameIgnoreCase(String name);

    @Query(
        """
        select distinct ledger
        from Ledger ledger
        join UserLedgerPermission permission on permission.ledger = ledger
        where permission.user = :user
          and permission.status = com.everycent.domain.enumeration.PermissionStatus.ACTIVE
        order by ledger.createdDate desc
        """
    )
    List<Ledger> findAllActiveLedgersForUser(@Param("user") User user);
}
