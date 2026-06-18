package com.everycent.repository;

import com.everycent.domain.EmotionTag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmotionTagRepository extends JpaRepository<EmotionTag, Long> {
    Optional<EmotionTag> findOneByCode(String code);

    List<EmotionTag> findAllByOrderByIdAsc();
}
