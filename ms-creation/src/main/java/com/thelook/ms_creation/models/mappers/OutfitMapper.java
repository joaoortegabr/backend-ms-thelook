package com.thelook.ms_creation.models.mappers;

import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.ItemResponse;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.dtos.OutfitResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface OutfitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "image1Url", ignore = true)
    @Mapping(target = "image2Url", ignore = true)
    @Mapping(target = "imageStatus", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Outfit toOutfit(OutfitRequest outfitRequest);

    @Mapping(source = "id", target = "outfitId")
    OutfitResponse toOutfitResponse(Outfit outfit);

    @Mapping(source = "id", target = "outfitId")
    OutfitSyncDTO toOutfitSyncDTO(Outfit outfit);

    List<Item> toEntityList(List<ItemRequest> items);
    List<ItemResponse> toItemList(List<Item> items);

}
