package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.VehicleCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface VehicleCategoryService {

    VehicleCategoryDTO.Detail save(
            VehicleCategoryDTO.SaveOrUpdate request,
            SystemUser user
    );

    VehicleCategoryDTO.Detail update(
            String uuid,
            VehicleCategoryDTO.SaveOrUpdate request,
            SystemUser user
    );

    VehicleCategoryDTO.Detail getById(String uuid);

    DataTableResponsePacket<VehicleCategoryDTO.Detail> getListData(
            VehicleCategoryDTO.ListOrSearchData request
    );

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );

    List<KeyValueDto> getKeyValueList(String lang);
}