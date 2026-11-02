package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.ShopGlobalCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.BusinessType;

import java.util.List;

public interface ShopGlobalCategoryService {

    ShopGlobalCategoryDTO.Detail save(
            ShopGlobalCategoryDTO.SaveOrUpdate request, SystemUser user);

    ShopGlobalCategoryDTO.Detail update(
            String uuid, ShopGlobalCategoryDTO.SaveOrUpdate request, SystemUser user);

    ShopGlobalCategoryDTO.Detail getById(String uuid);

    DataTableResponsePacket<ShopGlobalCategoryDTO.Detail> getListData(
            ShopGlobalCategoryDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);

    /** Dropdown used by the admin shop form (filtered by SHOP / RESTAURANT). */
    List<KeyValueDto> getKeyValueList(BusinessType businessType, String lang);
}
