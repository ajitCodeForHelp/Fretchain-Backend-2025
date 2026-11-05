package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.dto.VehicleDTO;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    // vendor / vehicle are resolved by the service; password is encoded there (blank keeps the old one)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "driverDocument", ignore = true)
    Driver toEntity(DriverDTO.SaveOrUpdate dto);

    @Mapping(target = "vendorUuid", source = "vendor.uuid")
    @Mapping(target = "vendorName", source = "vendor.fullName")
    DriverDTO.Detail toDetailDto(Driver driver);

    @Mapping(target = "vehicleCategoryUuid", source = "vehicleCategory.uuid")
    @Mapping(target = "vehicleCategoryNameEn", source = "vehicleCategory.nameEn")
    @Mapping(target = "vehicleCategoryNameFr", source = "vehicleCategory.nameFr")
    VehicleDTO.Basic toVehicleBasic(Vehicle vehicle);

    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "driverDocument", ignore = true)
    void updateEntityFromDto(
            DriverDTO.SaveOrUpdate dto,
            @MappingTarget Driver driver
    );
}
