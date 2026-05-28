package com.thelook.ms_feed.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thelook.ms_feed.entities.OutfitDocument;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedPageResponse(
        List<OutfitDocument> outfits,
        long total,
        List<Object> nextSortValues   // null = fim dos resultados
) {}