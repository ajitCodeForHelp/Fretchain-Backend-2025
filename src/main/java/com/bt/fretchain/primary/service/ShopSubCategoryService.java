package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.ShopSubCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface ShopSubCategoryService {

    ShopSubCategoryDTO.Detail save(
            ShopSubCategoryDTO.SaveOrUpdate request, SystemUser user);

    ShopSubCategoryDTO.Detail update(
            String uuid, ShopSubCategoryDTO.SaveOrUpdate request, SystemUser user);

    ShopSubCategoryDTO.Detail getById(String uuid);

    DataTableResponsePacket<ShopSubCategoryDTO.Detail> getListData(
            ShopSubCategoryDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);
}