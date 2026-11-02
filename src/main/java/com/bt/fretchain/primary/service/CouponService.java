package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.CouponDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface CouponService {

    CouponDTO.Detail save(
            CouponDTO.SaveOrUpdate request,
            SystemUser user
    );

    CouponDTO.Detail update(
            String uuid,
            CouponDTO.SaveOrUpdate request,
            SystemUser user
    );

    CouponDTO.Detail getById(String uuid);

    DataTableResponsePacket<CouponDTO.Detail> getListData(
            CouponDTO.ListOrSearchData request
    );

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );
}