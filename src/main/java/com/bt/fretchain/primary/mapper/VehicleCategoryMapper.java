package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.VehicleCategoryDTO;
import com.bt.fretchain.primary.pojo.VehicleCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleCategoryMapper {

    @Mapping(target = "vehicleType", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    VehicleCategory toEntity(VehicleCategoryDTO.SaveOrUpdate dto);
    VehicleCategoryDTO.Detail toDetailDto(VehicleCategory vehicleCategory);
    @Mapping(target = "vehicleType", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    void updateEntityFromDto(
            VehicleCategoryDTO.SaveOrUpdate dto,
            @MappingTarget VehicleCategory entity
    );
}