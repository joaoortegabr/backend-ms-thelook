package com.thelook.ms_creation.models.dtos;

import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.models.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ItemRequest(
        @NotNull ItemType itemType,
        @NotBlank @Size(max = 32) String itemName,
        String itemImg,
        @NotBlank @URL String itemUrl,
        ImageProcessStatus imageStatus
    ) {

}