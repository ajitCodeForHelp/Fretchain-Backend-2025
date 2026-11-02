package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.CustomerMarketplaceDTO;
import com.bt.fretchain.primary.pojo.*;
import org.mapstruct.*;

/** Entity -> customer facing cards. Computed fields are filled by the service. */
@Mapper(componentModel = "spring")
public interface CustomerMarketplaceMapper {

    CustomerMarketplaceDTO.GlobalCategoryCard toGlobalCategoryCard(
            ShopGlobalCategory entity);

    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "estimatedDeliveryMinutes", ignore = true)
    @Mapping(target = "openNow", ignore = true)
    CustomerMarketplaceDTO.ShopCard toShopCard(ShopMaster entity);

    @Mapping(target = "globalCategoryUuid", source = "globalCategory.uuid")
    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    @Mapping(target = "items", ignore = true)
    CustomerMarketplaceDTO.MenuCategory toMenuCategory(ShopCategory entity);

    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "items", ignore = true)
    CustomerMarketplaceDTO.MenuSubCategory toMenuSubCategory(
            ShopSubCategory entity);

    @Mapping(target = "categoryUuid", source = "category.uuid")
    @Mapping(target = "categoryName", source = "category.nameEn")
    @Mapping(target = "subCategoryUuid", source = "subCategory.uuid")
    @Mapping(target = "subCategoryName", source = "subCategory.nameEn")
    @Mapping(target = "shopUuid", ignore = true)
    @Mapping(target = "shopName", ignore = true)
    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "inStock", ignore = true)
    CustomerMarketplaceDTO.ItemCard toItemCard(ShopItem entity);

    @Mapping(target = "key", source = "uuid")
    @Mapping(target = "value", source = "nameEn")
    KeyValueDto toKeyValue(ShopGlobalCategory entity);
}
