package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.ShopItemDTO;
import com.bt.fretchain.primary.pojo.ShopItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShopItemMapper {

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    ShopItem toEntity(ShopItemDTO.SaveOrUpdate dto);

    @Mapping(target = "shopUuid", source = "shop.uuid")
    @Mapping(target = "shopName", source = "shop.nameEn")
    @Mapping(target = "categoryUuid", source = "category.uuid")
    @Mapping(target = "categoryName", source = "category.nameEn")
    @Mapping(target = "subCategoryUuid", source = "subCategory.uuid")
    @Mapping(target = "subCategoryName", source = "subCategory.nameEn")
    ShopItemDTO.Detail toDetailDto(ShopItem entity);

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    void updateEntityFromDto(
            ShopItemDTO.SaveOrUpdate dto,
            @MappingTarget ShopItem entity
    );
}