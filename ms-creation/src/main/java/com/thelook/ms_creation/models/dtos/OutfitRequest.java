package com.thelook.ms_creation.models.dtos;

import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.enums.OutfitColor;
import com.thelook.ms_creation.models.enums.OutfitStyle;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record OutfitRequest(
        UUID creatorId,
        String title,
        String image1Url,
        String image2Url,
        OutfitStyle style,
        Set<OutfitColor> colors,
        List<ItemRequest> items
    ) {

}