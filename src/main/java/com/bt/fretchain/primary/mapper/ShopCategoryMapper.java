package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.ShopCategoryDTO;
import com.bt.fretchain.primary.pojo.ShopCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShopCategoryMapper {

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "globalCategory", ignore = true)
    ShopCategory toEntity(ShopCategoryDTO.SaveOrUpdate dto);

    @Mapping(target = "shopUuid", source = "shop.uuid")
    @Mapping(target = "shopName", source = "shop.nameEn")
    @Mapping(target = "globalCategoryUuid", source = "globalCategory.uuid")
    @Mapping(target = "globalCategoryName", source = "globalCategory.nameEn")
    ShopCategoryDTO.Detail toDetailDto(ShopCategory entity);

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "globalCategory", ignore = true)
    void updateEntityFromDto(
            ShopCategoryDTO.SaveOrUpdate dto,
            @MappingTarget ShopCategory entity
    );
}
