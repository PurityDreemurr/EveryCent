package com.everycent.repository;

import com.everycent.domain.BehaviorTag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BehaviorTagRepository extends JpaRepository<BehaviorTag, Long> {
    Optional<BehaviorTag> findOneByCode(String code);

    List<BehaviorTag> findAllByOrderByIdAsc();
}
