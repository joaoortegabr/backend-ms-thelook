package com.thelook.ms_creation.models.mappers;

import com.thelook.dtos.ItemSyncDTO;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.ItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "outfit", ignore = true)
    @Mapping(target = "imageStatus", ignore = true)
    Item toItem(ItemRequest itemRequest);

    @Mapping(source = "id", target = "itemId")
    ItemResponse toItemResponse(Item item);

    @Mapping(source="id", target="itemId")
    ItemSyncDTO toItemSyncDTO(Item item);

}
