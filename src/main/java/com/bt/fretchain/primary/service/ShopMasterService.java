package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface ShopMasterService {

    ShopMasterDTO.Detail save(
            ShopMasterDTO.SaveOrUpdate request, SystemUser user);

    ShopMasterDTO.Detail update(
            String uuid, ShopMasterDTO.SaveOrUpdate request, SystemUser user);

    ShopMasterDTO.Detail getById(String uuid);

    DataTableResponsePacket<ShopMasterDTO.Detail> getListData(
            ShopMasterDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);

    List<KeyValueDto> getKeyValueList(String lang);
}