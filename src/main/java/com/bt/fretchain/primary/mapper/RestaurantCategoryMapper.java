package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.RestaurantCategoryDTO;
import com.bt.fretchain.primary.pojo.RestaurantCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RestaurantCategoryMapper {

    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "globalCategory", ignore = true)
    RestaurantCategory toEntity(RestaurantCategoryDTO.SaveOrUpdate dto);

    @Mapping(target = "restaurantUuid", source = "restaurant.uuid")
    @Mapping(target = "restaurantName", source = "restaurant.nameEn")
    @Mapping(target = "globalCategoryUuid", source = "globalCategory.uuid")
    @Mapping(target = "globalCategoryName", source = "globalCategory.nameEn")
    RestaurantCategoryDTO.Detail toDetailDto(RestaurantCategory entity);

    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "globalCategory", ignore = true)
    void updateEntityFromDto(
            RestaurantCategoryDTO.SaveOrUpdate dto,
            @MappingTarget RestaurantCategory entity
    );
}
