package com.thelook.ms_creation.models.dtos;

import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.models.enums.ItemType;

import java.util.UUID;

public record ItemResponse(
        UUID itemId,
        ItemType itemType,
        String itemName,
        String itemImg,
        String itemUrl,
        ImageProcessStatus imageStatus
    ) {

}