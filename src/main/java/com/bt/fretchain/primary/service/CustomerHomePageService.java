package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.CustomerHomePageDTO;

import java.util.List;

public interface CustomerHomePageService {

    List<CustomerHomePageDTO.CategoryResponse> getCategoryList();

    List<CustomerHomePageDTO.VehicleCategoryResponse>
    getVehicleCategoryList(
            CustomerHomePageDTO.VehicleCategoryFareRequest request
    );

    CustomerHomePageDTO.CheckServiceResponse checkService(
            CustomerHomePageDTO.CheckServiceRequest request
    );
}