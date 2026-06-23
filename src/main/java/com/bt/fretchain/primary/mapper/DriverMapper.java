package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.pojo.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    Driver toEntity(DriverDTO.SaveOrUpdate dto);
    DriverDTO.Detail toDetailDto(Driver driver);
    void updateEntityFromDto(
            DriverDTO.SaveOrUpdate dto,
            @MappingTarget Driver driver
    );
}