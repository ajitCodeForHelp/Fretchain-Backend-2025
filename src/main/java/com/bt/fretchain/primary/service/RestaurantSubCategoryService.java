package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.RestaurantSubCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface RestaurantSubCategoryService {

    RestaurantSubCategoryDTO.Detail save(
            RestaurantSubCategoryDTO.SaveOrUpdate request, SystemUser user);

    RestaurantSubCategoryDTO.Detail update(
            String uuid, RestaurantSubCategoryDTO.SaveOrUpdate request, SystemUser user);

    RestaurantSubCategoryDTO.Detail getById(String uuid);

    DataTableResponsePacket<RestaurantSubCategoryDTO.Detail> getListData(
            RestaurantSubCategoryDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);
}