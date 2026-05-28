package com.thelook.ms_feed.controllers;

import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.models.dtos.FeedPageResponse;
import com.thelook.ms_feed.services.FeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/outfits")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/feed")
    public ResponseEntity<FeedPageResponse> getFeed(
            @RequestParam(required = false) String style,
            @RequestParam(required = false) List<String> colors,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<Object> lastSortValues) {

        SearchHits<OutfitDocument> hits = feedService.searchFeed(style, colors, itemType, title, size, lastSortValues);

        List<OutfitDocument> outfits = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        List<Object> nextCursor = outfits.size() < size
                ? null
                : hits.getSearchHits().getLast().getSortValues();

        return ResponseEntity.ok(new FeedPageResponse(outfits, hits.getTotalHits(), nextCursor));
    }

    @GetMapping("/{outfitId}")
    public ResponseEntity<OutfitDocument> getById(@PathVariable String outfitId) {
        return feedService.findById(outfitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<FeedPageResponse> getByCreator(
            @PathVariable UUID creatorId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<Object> lastSortValues) {

        SearchHits<OutfitDocument> hits = feedService.findByCreatorId(creatorId, size, lastSortValues);

        List<OutfitDocument> outfits = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        List<Object> nextCursor = outfits.size() < size
                ? null
                : hits.getSearchHits().getLast().getSortValues();

        return ResponseEntity.ok(new FeedPageResponse(outfits, hits.getTotalHits(), nextCursor));
    }
}