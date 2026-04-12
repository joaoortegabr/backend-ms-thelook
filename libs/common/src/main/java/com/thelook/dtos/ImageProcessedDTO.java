package com.thelook.dtos;

import com.thelook.enums.ImageProcessStatus;

import java.util.UUID;

public record ImageProcessedDTO(
        UUID outfitId,
        String originalPath,
        String processedPath,
        String type, // OUTFIT ou ITEM
        ImageProcessStatus imageStatus
) {}
