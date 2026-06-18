package com.everycent.assistant.emotion;

import com.everycent.domain.AiEmotionState;
import org.springframework.stereotype.Service;

@Service
public class AiEmotionStateService {

    public AiEmotionState initializeDefaultState() {
        return AiEmotionStateModel.defaultState();
    }

    public AiEmotionState normalize(AiEmotionState state) {
        if (state == null || state.getCurrentEmotion() == null) {
            return initializeDefaultState();
        }
        return state;
    }
}
