package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.enumeration.EmotionValence;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.service.dto.BehaviorTagDTO;
import com.everycent.service.dto.EmotionTagDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagServiceTest {

    private BehaviorTagRepository behaviorTagRepository;

    private EmotionTagRepository emotionTagRepository;

    private TagService service;

    @BeforeEach
    void setUp() {
        behaviorTagRepository = org.mockito.Mockito.mock(BehaviorTagRepository.class);
        emotionTagRepository = org.mockito.Mockito.mock(EmotionTagRepository.class);
        service = new TagService(behaviorTagRepository, emotionTagRepository);
    }

    @Test
    void findBehaviorTagsShouldReturnOrderedDtos() {
        BehaviorTag tag = new BehaviorTag();
        tag.setId(1L);
        tag.setCode("FOOD");
        tag.setName("Food");
        tag.setSystemDefault(true);
        when(behaviorTagRepository.findAllByOrderByIdAsc()).thenReturn(List.of(tag));

        List<BehaviorTagDTO> result = service.findBehaviorTags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getCode()).isEqualTo("FOOD");
        assertThat(result.get(0).getName()).isEqualTo("Food");
        assertThat(result.get(0).getSystemDefault()).isTrue();
    }

    @Test
    void findEmotionTagsShouldReturnOrderedDtos() {
        EmotionTag tag = new EmotionTag();
        tag.setId(2L);
        tag.setCode("HAPPY");
        tag.setName("Happy");
        tag.setValence(EmotionValence.POSITIVE);
        tag.setSystemDefault(true);
        when(emotionTagRepository.findAllByOrderByIdAsc()).thenReturn(List.of(tag));

        List<EmotionTagDTO> result = service.findEmotionTags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(0).getCode()).isEqualTo("HAPPY");
        assertThat(result.get(0).getName()).isEqualTo("Happy");
        assertThat(result.get(0).getValence()).isEqualTo(EmotionValence.POSITIVE);
        assertThat(result.get(0).getSystemDefault()).isTrue();
    }
}
