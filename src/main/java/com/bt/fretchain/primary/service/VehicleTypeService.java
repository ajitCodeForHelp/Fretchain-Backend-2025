package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.VehicleTypeDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface VehicleTypeService {

    VehicleTypeDTO.Detail save(
            VehicleTypeDTO.SaveOrUpdate request,
            SystemUser user
    );

    VehicleTypeDTO.Detail update(
            String uuid,
            VehicleTypeDTO.SaveOrUpdate request,
            SystemUser user
    );

    VehicleTypeDTO.Detail getById(String uuid);

    DataTableResponsePacket<VehicleTypeDTO.Detail> getListData(
            VehicleTypeDTO.ListOrSearchData request
    );

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );

    List<KeyValueDto> getKeyValueList(String lang);
}