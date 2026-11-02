package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.pojo.ShopMaster;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShopMasterMapper {

    @Mapping(target = "owner", ignore = true)
    ShopMaster toEntity(ShopMasterDTO.SaveOrUpdate dto);

    @Mapping(target = "ownerUuid", source = "owner.uuid")
    @Mapping(target = "ownerName", ignore = true)
    ShopMasterDTO.Detail toDetailDto(ShopMaster entity);

    @Mapping(target = "owner", ignore = true)
    void updateEntityFromDto(
            ShopMasterDTO.SaveOrUpdate dto,
            @MappingTarget ShopMaster entity
    );
}