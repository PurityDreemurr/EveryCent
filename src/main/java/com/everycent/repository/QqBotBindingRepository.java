package com.everycent.repository;

import com.everycent.domain.QqBotBinding;
import com.everycent.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QqBotBindingRepository extends JpaRepository<QqBotBinding, Long> {
    Optional<QqBotBinding> findOneByUser(User user);
    Optional<QqBotBinding> findOneByBindingCode(String bindingCode);
    Optional<QqBotBinding> findOneByQqOpenId(String qqOpenId);
    boolean existsByBindingCode(String bindingCode);
}
