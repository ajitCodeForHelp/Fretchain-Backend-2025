package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.SubCategoryDTO;
import com.bt.fretchain.primary.pojo.SubCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubCategoryMapper {

    @Mapping(target = "category", ignore = true)
    SubCategory toEntity(SubCategoryDTO.SaveOrUpdate dto);

    SubCategoryDTO.Detail toDetailDto(SubCategory entity);

    @Mapping(target = "category", ignore = true)
    void updateEntityFromDto(
            SubCategoryDTO.SaveOrUpdate dto,
            @MappingTarget SubCategory entity
    );
}