package com.thelook.ms_feed.controllers;

import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.services.FeedService;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outfits")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getFeed(
            @RequestParam(required = false) String style,
            @RequestParam(required = false) List<String> colors,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<Object> lastSortValues) {

        SearchHits<OutfitDocument> hits = feedService.searchFeed(style, colors, itemType, title, size, lastSortValues);

        List<OutfitDocument> outfits = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        List<Object> nextSortValues = hits.getSearchHits().isEmpty()
                ? null
                : hits.getSearchHits().getLast().getSortValues();

        return ResponseEntity.ok(Map.of(
                "outfits", outfits,
                "total", hits.getTotalHits(),
                "nextSortValues", nextSortValues != null ? nextSortValues : List.of()
        ));
    }

    @GetMapping("/{outfitId}")
    public ResponseEntity<OutfitDocument> getById(@PathVariable String outfitId) {
        return feedService.findById(outfitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<OutfitDocument>> getByCreator(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(feedService.findByCreatorId(creatorId));
    }
}