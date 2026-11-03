package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.RestaurantSubCategoryDTO;
import com.bt.fretchain.primary.pojo.RestaurantSubCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RestaurantSubCategoryMapper {

    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    RestaurantSubCategory toEntity(RestaurantSubCategoryDTO.SaveOrUpdate dto);

    @Mapping(target = "restaurantUuid", source = "restaurant.uuid")
    @Mapping(target = "restaurantName", source = "restaurant.nameEn")
    @Mapping(target = "categoryUuid", source = "category.uuid")
    @Mapping(target = "categoryName", source = "category.nameEn")
    RestaurantSubCategoryDTO.Detail toDetailDto(RestaurantSubCategory entity);

    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntityFromDto(
            RestaurantSubCategoryDTO.SaveOrUpdate dto,
            @MappingTarget RestaurantSubCategory entity
    );
}