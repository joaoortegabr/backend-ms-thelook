package com.thelook.dtos;

import java.util.UUID;
import java.util.Set;
import java.util.List;

import com.thelook.dtos.ItemSyncDTO;
import com.thelook.enums.ImageProcessStatus;

public record OutfitSyncDTO(
        UUID outfitId,
        UUID creatorId,
        String title,
        String image1Url,
        String image2Url,
        String style,
        Set<String> colors,
        List<ItemSyncDTO> items,
        ImageProcessStatus imageStatus
) {}
