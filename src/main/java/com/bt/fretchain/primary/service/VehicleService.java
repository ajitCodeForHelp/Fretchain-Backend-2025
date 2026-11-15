package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.VehicleDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface VehicleService {

    VehicleDTO.Detail save(
            VehicleDTO.SaveOrUpdate request,
            SystemUser user
    );

    VehicleDTO.Detail update(
            String uuid,
            VehicleDTO.SaveOrUpdate request,
            SystemUser user
    );

    VehicleDTO.Detail getById(String uuid);

    DataTableResponsePacket<VehicleDTO.Detail> getListData(
            VehicleDTO.ListOrSearchData request
    );

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );

    List<KeyValueDto> getKeyValueList();

    /** Active records of one vendor (blank = all). */
    List<KeyValueDto> getKeyValueList(String vendorUuid);

    /** Approve / reject (reason required) / back to pending. */
    VehicleDTO.Detail review(String uuid, com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus status, String reason, SystemUser user);

    /** A fleet owner corrected a rejected vehicle: it goes back to PENDING for a new review. */
    void resubmitIfRejected(String uuid);
}
