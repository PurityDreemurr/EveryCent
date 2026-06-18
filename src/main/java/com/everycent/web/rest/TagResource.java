package com.everycent.web.rest;

import com.everycent.service.TagService;
import com.everycent.service.dto.BehaviorTagDTO;
import com.everycent.service.dto.EmotionTagDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TagResource {

    private static final Logger LOG = LoggerFactory.getLogger(TagResource.class);

    private final TagService tagService;

    public TagResource(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping({ "/tags/behavior", "/tags/behavior/" })
    public ResponseEntity<List<BehaviorTagDTO>> getBehaviorTags() {
        LOG.debug("REST request to get behavior tags");
        return ResponseEntity.ok(tagService.findBehaviorTags());
    }

    @GetMapping({ "/tags/emotion", "/tags/emotion/" })
    public ResponseEntity<List<EmotionTagDTO>> getEmotionTags() {
        LOG.debug("REST request to get emotion tags");
        return ResponseEntity.ok(tagService.findEmotionTags());
    }
}
