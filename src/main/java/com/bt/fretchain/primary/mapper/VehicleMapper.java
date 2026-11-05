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
    @Mapping(target = "driver", ignore = true)
    Vehicle toEntity(VehicleDTO.SaveOrUpdate dto);

    @Mapping(target = "vendorUuid", source = "vendor.uuid")
    @Mapping(target = "vendorName", source = "vendor.fullName")
    @Mapping(target = "driverUuid", source = "driver.uuid")
    @Mapping(target = "driverName", source = "driver.fullName")
    VehicleDTO.Detail toDetailDto(Vehicle vehicle);

    @Mapping(target = "vehicleCategory", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "driver", ignore = true)
    void updateEntityFromDto(
            VehicleDTO.SaveOrUpdate dto,
            @MappingTarget Vehicle entity
    );
}
