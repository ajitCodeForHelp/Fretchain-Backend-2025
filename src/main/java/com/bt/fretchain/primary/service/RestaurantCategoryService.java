package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.RestaurantCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface RestaurantCategoryService {

    RestaurantCategoryDTO.Detail save(
            RestaurantCategoryDTO.SaveOrUpdate request, SystemUser user);

    RestaurantCategoryDTO.Detail update(
            String uuid, RestaurantCategoryDTO.SaveOrUpdate request, SystemUser user);

    RestaurantCategoryDTO.Detail getById(String uuid);

    DataTableResponsePacket<RestaurantCategoryDTO.Detail> getListData(
            RestaurantCategoryDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);
}