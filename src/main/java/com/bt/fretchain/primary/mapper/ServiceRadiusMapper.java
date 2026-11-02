package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.ServiceRadiusDTO;
import com.bt.fretchain.primary.pojo.ServiceRadius;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ServiceRadiusMapper {

    ServiceRadius toEntity(
            ServiceRadiusDTO.SaveOrUpdate dto
    );

    ServiceRadiusDTO.Detail toDetailDto(
            ServiceRadius entity
    );

    void updateEntityFromDto(
            ServiceRadiusDTO.SaveOrUpdate dto,
            @MappingTarget ServiceRadius entity
    );
}