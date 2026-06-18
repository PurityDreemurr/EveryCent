package com.everycent.service;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.service.dto.BehaviorTagDTO;
import com.everycent.service.dto.EmotionTagDTO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final BehaviorTagRepository behaviorTagRepository;

    private final EmotionTagRepository emotionTagRepository;

    public TagService(BehaviorTagRepository behaviorTagRepository, EmotionTagRepository emotionTagRepository) {
        this.behaviorTagRepository = behaviorTagRepository;
        this.emotionTagRepository = emotionTagRepository;
    }

    public List<BehaviorTagDTO> findBehaviorTags() {
        return behaviorTagRepository.findAllByOrderByIdAsc().stream().map(this::toBehaviorTagDTO).toList();
    }

    public List<EmotionTagDTO> findEmotionTags() {
        return emotionTagRepository.findAllByOrderByIdAsc().stream().map(this::toEmotionTagDTO).toList();
    }

    private BehaviorTagDTO toBehaviorTagDTO(BehaviorTag tag) {
        BehaviorTagDTO dto = new BehaviorTagDTO();
        dto.setId(tag.getId());
        dto.setCode(tag.getCode());
        dto.setName(tag.getName());
        dto.setSystemDefault(tag.getSystemDefault());
        return dto;
    }

    private EmotionTagDTO toEmotionTagDTO(EmotionTag tag) {
        EmotionTagDTO dto = new EmotionTagDTO();
        dto.setId(tag.getId());
        dto.setCode(tag.getCode());
        dto.setName(tag.getName());
        dto.setValence(tag.getValence());
        dto.setSystemDefault(tag.getSystemDefault());
        return dto;
    }
}
