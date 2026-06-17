package com.everycent.repository;

import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.PermissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLedgerPermissionRepository extends JpaRepository<UserLedgerPermission, Long> {
    Optional<UserLedgerPermission> findOneByUserAndLedgerAndStatus(User user, Ledger ledger, PermissionStatus status);

    List<UserLedgerPermission> findAllByUserAndStatus(User user, PermissionStatus status);

    List<UserLedgerPermission> findAllByLedgerAndStatus(Ledger ledger, PermissionStatus status);

    void deleteAllByLedger(Ledger ledger);
}
