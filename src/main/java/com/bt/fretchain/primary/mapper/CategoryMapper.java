package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.CategoryDTO;
import com.bt.fretchain.primary.pojo.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toEntity(CategoryDTO.SaveOrUpdate dto);

    CategoryDTO.Detail toDetailDto(Category category);

    void updateEntityFromDto(
            CategoryDTO.SaveOrUpdate dto,
            @MappingTarget Category entity
    );
}