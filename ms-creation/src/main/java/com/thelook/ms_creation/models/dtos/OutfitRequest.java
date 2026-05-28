package com.thelook.ms_creation.models.dtos;

import com.thelook.ms_creation.models.enums.OutfitColor;
import com.thelook.ms_creation.models.enums.OutfitStyle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record OutfitRequest(
        @NotBlank @Size(max = 64) String title,
        @NotNull OutfitStyle style,
        @NotEmpty Set<OutfitColor> colors,
        @Valid @Size(max = 6) List<ItemRequest> items
    ) {

}