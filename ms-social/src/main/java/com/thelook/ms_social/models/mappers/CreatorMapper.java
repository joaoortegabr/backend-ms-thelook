package com.thelook.ms_social.mappers;

import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.models.dtos.CreatorRequest;
import com.thelook.ms_social.models.dtos.CreatorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")
public interface CreatorMapper {

    @Mapping(target = "id", ignore = true)
    Creator toCreator(CreatorRequest creatorRequest);

    CreatorResponse toCreatorResponse(Creator creator);

}
