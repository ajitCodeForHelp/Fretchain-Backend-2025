package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.ShopCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface ShopCategoryService {

    ShopCategoryDTO.Detail save(
            ShopCategoryDTO.SaveOrUpdate request, SystemUser user);

    ShopCategoryDTO.Detail update(
            String uuid, ShopCategoryDTO.SaveOrUpdate request, SystemUser user);

    ShopCategoryDTO.Detail getById(String uuid);

    DataTableResponsePacket<ShopCategoryDTO.Detail> getListData(
            ShopCategoryDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);
}