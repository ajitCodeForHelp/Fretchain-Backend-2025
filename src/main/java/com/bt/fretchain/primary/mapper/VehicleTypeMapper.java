package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.VehicleTypeDTO;
import com.bt.fretchain.primary.pojo.VehicleType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleTypeMapper {

    VehicleType toEntity(VehicleTypeDTO.SaveOrUpdate dto);

    VehicleTypeDTO.Detail toDetailDto(VehicleType vehicleType);

    void updateEntityFromDto(
            VehicleTypeDTO.SaveOrUpdate dto,
            @MappingTarget VehicleType entity
    );
}