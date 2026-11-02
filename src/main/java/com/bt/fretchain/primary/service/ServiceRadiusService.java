package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.ServiceRadiusDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface ServiceRadiusService {

    ServiceRadiusDTO.Detail save(
            ServiceRadiusDTO.SaveOrUpdate request,
            SystemUser user
    );

    ServiceRadiusDTO.Detail update(
            String uuid,
            ServiceRadiusDTO.SaveOrUpdate request,
            SystemUser user
    );

    ServiceRadiusDTO.Detail getById(String uuid);

    DataTableResponsePacket<ServiceRadiusDTO.Detail> getListData(
            ServiceRadiusDTO.ListOrSearchData request
    );

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );
}