package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.ShopSubCategoryDTO;
import com.bt.fretchain.primary.pojo.ShopSubCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShopSubCategoryMapper {

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "category", ignore = true)
    ShopSubCategory toEntity(ShopSubCategoryDTO.SaveOrUpdate dto);

    @Mapping(target = "shopUuid", source = "shop.uuid")
    @Mapping(target = "shopName", source = "shop.nameEn")
    @Mapping(target = "categoryUuid", source = "category.uuid")
    @Mapping(target = "categoryName", source = "category.nameEn")
    ShopSubCategoryDTO.Detail toDetailDto(ShopSubCategory entity);

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntityFromDto(
            ShopSubCategoryDTO.SaveOrUpdate dto,
            @MappingTarget ShopSubCategory entity
    );
}