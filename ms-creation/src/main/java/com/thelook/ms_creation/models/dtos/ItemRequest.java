package com.thelook.ms_creation.models.dtos;

import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.enums.ItemType;

public record ItemRequest(
        Outfit outfit,
        ItemType itemType,
        String itemName,
        String itemImg,
        String itemUrl
    ) {

}