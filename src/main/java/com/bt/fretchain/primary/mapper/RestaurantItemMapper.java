package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.RestaurantItemDTO;
import com.bt.fretchain.primary.pojo.RestaurantItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RestaurantItemMapper {

    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    RestaurantItem toEntity(RestaurantItemDTO.SaveOrUpdate dto);

    @Mapping(target = "restaurantUuid", source = "restaurant.uuid")
    @Mapping(target = "restaurantName", source = "restaurant.nameEn")
    @Mapping(target = "categoryUuid", source = "category.uuid")
    @Mapping(target = "categoryName", source = "category.nameEn")
    @Mapping(target = "subCategoryUuid", source = "subCategory.uuid")
    @Mapping(target = "subCategoryName", source = "subCategory.nameEn")
    RestaurantItemDTO.Detail toDetailDto(RestaurantItem entity);

    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    void updateEntityFromDto(
            RestaurantItemDTO.SaveOrUpdate dto,
            @MappingTarget RestaurantItem entity
    );
}