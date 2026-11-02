package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.dto.AvailableDriverResponse;

import java.util.List;

public interface DriverRepositoryCustom {
    List<AvailableDriverResponse> getAvailableDriversForOrder(
            Long orderId
    );
}