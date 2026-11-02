package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.RestaurantGlobalCategoryDTO;
import com.bt.fretchain.primary.pojo.RestaurantGlobalCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RestaurantGlobalCategoryMapper {

    RestaurantGlobalCategory toEntity(RestaurantGlobalCategoryDTO.SaveOrUpdate dto);

    RestaurantGlobalCategoryDTO.Detail toDetailDto(RestaurantGlobalCategory entity);

    void updateEntityFromDto(
            RestaurantGlobalCategoryDTO.SaveOrUpdate dto,
            @MappingTarget RestaurantGlobalCategory entity
    );
}
