package com.thelook.dtos;

import java.util.UUID;

import com.thelook.enums.ImageProcessStatus;

public record ItemSyncDTO(
        UUID itemId,
        String itemName,
        String itemType,
        String itemImg,
        String itemUrl,
        ImageProcessStatus imageStatus
) {}