package com.everycent.repository;

import com.everycent.domain.AiConversation;
import com.everycent.domain.AiMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findAllByConversationOrderByCreatedDateAscIdAsc(AiConversation conversation);

    List<AiMessage> findTop12ByConversationOrderByCreatedDateDescIdDesc(AiConversation conversation);
}
