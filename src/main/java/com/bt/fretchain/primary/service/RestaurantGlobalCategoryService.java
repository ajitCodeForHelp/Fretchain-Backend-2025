package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.RestaurantGlobalCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface RestaurantGlobalCategoryService {

    RestaurantGlobalCategoryDTO.Detail save(
            RestaurantGlobalCategoryDTO.SaveOrUpdate request, SystemUser user);

    RestaurantGlobalCategoryDTO.Detail update(
            String uuid, RestaurantGlobalCategoryDTO.SaveOrUpdate request, SystemUser user);

    RestaurantGlobalCategoryDTO.Detail getById(String uuid);

    DataTableResponsePacket<RestaurantGlobalCategoryDTO.Detail> getListData(
            RestaurantGlobalCategoryDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);

    /** Dropdown used by the admin restaurant form. */
    List<KeyValueDto> getKeyValueList(String lang);
}
