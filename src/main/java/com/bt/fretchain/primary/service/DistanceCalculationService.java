package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.CustomerHomePageDTO;
import com.bt.fretchain.primary.dto.DistanceTimeDTO;

import java.util.List;

public interface DistanceCalculationService {

    boolean isLocationInsideServiceRadius(
            Double latitude,
            Double longitude
    );

    DistanceTimeDTO calculateByGoogle(
            Double pickupLat,
            Double pickupLng,
            Double dropLat,
            Double dropLng
    );

    DistanceTimeDTO calculateByHaversine(
            Double pickupLat,
            Double pickupLng,
            Double dropLat,
            Double dropLng
    );

    DistanceTimeDTO calculateMultiDropDistance(
            Double pickupLat,
            Double pickupLng,
            List<CustomerHomePageDTO.DropLocationRequest> dropLocations
    );
}