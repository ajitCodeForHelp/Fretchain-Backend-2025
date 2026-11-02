package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.ShopItemDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface ShopItemService {

    ShopItemDTO.Detail save(
            ShopItemDTO.SaveOrUpdate request, SystemUser user);

    ShopItemDTO.Detail update(
            String uuid, ShopItemDTO.SaveOrUpdate request, SystemUser user);

    ShopItemDTO.Detail getById(String uuid);

    DataTableResponsePacket<ShopItemDTO.Detail> getListData(
            ShopItemDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);
}