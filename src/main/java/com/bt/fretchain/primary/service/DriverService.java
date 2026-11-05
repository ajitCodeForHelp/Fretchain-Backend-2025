package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface DriverService {

    DriverDTO.Detail save(
            DriverDTO.SaveOrUpdate request,
            SystemUser user
    );

    DriverDTO.Detail update(
            String uuid,
            DriverDTO.SaveOrUpdate request,
            SystemUser user
    );

    DriverDTO.Detail getById(String uuid);

    DataTableResponsePacket<DriverDTO.Detail> getListData(
            DriverDTO.ListOrSearchData request
    );

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );

    List<KeyValueDto> getKeyValueList();

    /** Active records of one vendor (blank = all). */
    List<KeyValueDto> getKeyValueList(String vendorUuid);
}