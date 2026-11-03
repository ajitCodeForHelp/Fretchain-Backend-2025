package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.pojo.ShopGlobalCategory;
import com.bt.fretchain.primary.pojo.ShopMaster;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShopMasterMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "globalCategories", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "statusChangedAt", ignore = true)
    @Mapping(target = "selfRegistered", ignore = true)
    ShopMaster toEntity(ShopMasterDTO.SaveOrUpdate dto);

    @Mapping(target = "ownerUuid", source = "owner.uuid")
    @Mapping(target = "ownerName", ignore = true)
    ShopMasterDTO.Detail toDetailDto(ShopMaster entity);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "globalCategories", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "statusChangedAt", ignore = true)
    @Mapping(target = "selfRegistered", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromDto(
            ShopMasterDTO.SaveOrUpdate dto,
            @MappingTarget ShopMaster entity
    );

    @Mapping(target = "key", source = "uuid")
    @Mapping(target = "value", source = "nameEn")
    KeyValueDto toKeyValue(ShopGlobalCategory entity);
}
