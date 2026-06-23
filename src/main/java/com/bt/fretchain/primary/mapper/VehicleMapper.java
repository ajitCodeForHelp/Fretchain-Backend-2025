package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.VehicleDTO;
import com.bt.fretchain.primary.pojo.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "vehicleCategory", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    Vehicle toEntity(VehicleDTO.SaveOrUpdate dto);


    VehicleDTO.Detail toDetailDto(Vehicle vehicle);

    @Mapping(target = "vehicleCategory", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    void updateEntityFromDto(
            VehicleDTO.SaveOrUpdate dto,
            @MappingTarget Vehicle entity
    );
}