package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.ShopGlobalCategoryDTO;
import com.bt.fretchain.primary.pojo.ShopGlobalCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShopGlobalCategoryMapper {

    ShopGlobalCategory toEntity(ShopGlobalCategoryDTO.SaveOrUpdate dto);

    ShopGlobalCategoryDTO.Detail toDetailDto(ShopGlobalCategory entity);

    void updateEntityFromDto(
            ShopGlobalCategoryDTO.SaveOrUpdate dto,
            @MappingTarget ShopGlobalCategory entity
    );
}
