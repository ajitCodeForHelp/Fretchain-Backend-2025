package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.RestaurantMasterDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface RestaurantMasterService {

    RestaurantMasterDTO.Detail save(
            RestaurantMasterDTO.SaveOrUpdate request, SystemUser user);

    RestaurantMasterDTO.Detail update(
            String uuid, RestaurantMasterDTO.SaveOrUpdate request, SystemUser user);

    RestaurantMasterDTO.Detail getById(String uuid);

    DataTableResponsePacket<RestaurantMasterDTO.Detail> getListData(
            RestaurantMasterDTO.ListOrSearchData request);

    void updateStatus(String uuid, Boolean active, SystemUser user);

    List<KeyValueDto> getKeyValueList(String lang);
}