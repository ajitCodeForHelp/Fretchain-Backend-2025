package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.AdminFreightOrderDTO;
import com.bt.fretchain.primary.dto.AvailableDriverResponse;
import com.bt.fretchain.primary.dto.DriverAssignmentDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;

import java.util.List;

public interface AdminFreightOrderService {

    DataTableResponsePacket<AdminFreightOrderDTO.ListResponse> getListData(
            AdminFreightOrderDTO.ListRequest request
    );

    AdminFreightOrderDTO.DetailResponse getById(
            String orderUuid
    );

    void updateStatus(
            String orderUuid,
            OrderStatus status,
            String remarks,
            SystemUser user
    );

     List<AvailableDriverResponse> getAvailableDrivers(
            String orderUuid,
            SystemUser user
    );

    DriverAssignmentDTO.AssignResponse assignDriver(
            String orderUuid,
            DriverAssignmentDTO.AssignRequest request,
            SystemUser user
    );

}