package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.RestaurantItemDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface RestaurantItemService {

    RestaurantItemDTO.Detail save(
            RestaurantItemDTO.SaveOrUpdate request, SystemUser user);

    RestaurantItemDTO.Detail update(
            String uuid, RestaurantItemDTO.SaveOrUpdate request, SystemUser user);

    RestaurantItemDTO.Detail getById(String uuid);

    DataTableResponsePacket<RestaurantItemDTO.Detail> getListData(
            RestaurantItemDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);
}