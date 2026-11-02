package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.CustomerRestaurantDTO;
import com.bt.fretchain.primary.pojo.*;
import org.mapstruct.*;

/** Restaurant entities -> customer facing cards. Computed fields are filled by the service. */
@Mapper(componentModel = "spring")
public interface CustomerRestaurantMapper {

    CustomerRestaurantDTO.CuisineCard toCuisineCard(RestaurantGlobalCategory entity);

    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "estimatedDeliveryMinutes", ignore = true)
    @Mapping(target = "openNow", ignore = true)
    CustomerRestaurantDTO.RestaurantCard toRestaurantCard(RestaurantMaster entity);

    @Mapping(target = "globalCategoryUuid", source = "globalCategory.uuid")
    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    @Mapping(target = "items", ignore = true)
    CustomerRestaurantDTO.MenuCategory toMenuCategory(RestaurantCategory entity);

    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "items", ignore = true)
    CustomerRestaurantDTO.MenuSubCategory toMenuSubCategory(RestaurantSubCategory entity);

    @Mapping(target = "categoryUuid", source = "category.uuid")
    @Mapping(target = "categoryName", source = "category.nameEn")
    @Mapping(target = "subCategoryUuid", source = "subCategory.uuid")
    @Mapping(target = "subCategoryName", source = "subCategory.nameEn")
    @Mapping(target = "restaurantUuid", ignore = true)
    @Mapping(target = "restaurantName", ignore = true)
    @Mapping(target = "distanceKm", ignore = true)
    CustomerRestaurantDTO.DishCard toDishCard(RestaurantItem entity);

    @Mapping(target = "key", source = "uuid")
    @Mapping(target = "value", source = "nameEn")
    KeyValueDto toKeyValue(RestaurantGlobalCategory entity);
}
