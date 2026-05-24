package com.thelook.ms_creation.models.dtos;

import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.models.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ItemRequest(
        @NotNull ItemType itemType,
        @NotBlank String itemName,
        String itemImg,
        @NotBlank String itemUrl,
        ImageProcessStatus imageStatus
    ) {

}