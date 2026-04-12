package com.thelook.ms_creation.models.dtos;

import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.enums.ItemType;

public record ItemResponse(
        Outfit outfit,
        ItemType itemType,
        String itemName,
        String itemImg,
        String itemUrl,
        ImageProcessStatus imageStatus
    ) {

}