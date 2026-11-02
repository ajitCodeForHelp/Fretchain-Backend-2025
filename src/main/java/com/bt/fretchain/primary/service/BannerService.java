package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.BannerDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface BannerService {

    BannerDTO.Detail save(
            BannerDTO.SaveOrUpdate request,
            SystemUser user
    );

    BannerDTO.Detail update(
            String uuid,
            BannerDTO.SaveOrUpdate request,
            SystemUser user
    );

    BannerDTO.Detail getById(String uuid);

    DataTableResponsePacket<BannerDTO.Detail> getListData(
            BannerDTO.ListOrSearchData request
    );

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );
}