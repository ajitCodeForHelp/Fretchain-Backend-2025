package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.RestaurantMasterDTO;
import com.bt.fretchain.primary.pojo.RestaurantGlobalCategory;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RestaurantMasterMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "globalCategories", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "statusChangedAt", ignore = true)
    @Mapping(target = "selfRegistered", ignore = true)
    RestaurantMaster toEntity(RestaurantMasterDTO.SaveOrUpdate dto);

    @Mapping(target = "ownerUuid", source = "owner.uuid")
    @Mapping(target = "ownerName", source = "owner.fullName")
    RestaurantMasterDTO.Detail toDetailDto(RestaurantMaster entity);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "globalCategories", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "statusChangedAt", ignore = true)
    @Mapping(target = "selfRegistered", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromDto(
            RestaurantMasterDTO.SaveOrUpdate dto,
            @MappingTarget RestaurantMaster entity
    );

    @Mapping(target = "key", source = "uuid")
    @Mapping(target = "value", source = "nameEn")
    KeyValueDto toKeyValue(RestaurantGlobalCategory entity);
}
