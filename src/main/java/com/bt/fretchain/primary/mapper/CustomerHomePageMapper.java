package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.CustomerHomePageDTO;
import com.bt.fretchain.primary.pojo.Category;
import com.bt.fretchain.primary.pojo.SubCategory;
import com.bt.fretchain.primary.pojo.VehicleCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerHomePageMapper {

    CustomerHomePageDTO.CategoryResponse toCategoryResponse(
            Category category
    );
    CustomerHomePageDTO.SubCategoryResponse toSubCategoryResponse(
            SubCategory subCategory
    );

    CustomerHomePageDTO.VehicleCategoryResponse toVehicleCategoryResponse(
            VehicleCategory vehicleCategory
    );
}