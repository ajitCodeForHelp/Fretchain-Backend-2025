package com.bt.fretchain.primary.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDriverResponse {

    private Long driverId;

    private String driverUuid;

    private String vendorName;

    private String fullName;

    private String mobile;

    private String countryCode;

    private String profileImageUrl;

    private Boolean verified;

    private Boolean available;

    private Boolean onlineStatus;

    private BigDecimal overallRating;

    private Double distanceKm;

    private Double currentLatitude;

    private Double currentLongitude;

    private LocalDateTime lastLocationUpdatedAt;

    private Long vehicleId;

    private String vehicleNumber;

    private Long vehicleCategoryId;

    private String vehicleCategoryNameEn;

    private String vehicleCategoryNameFr;

    private Long vehicleTypeId;

    private String vehicleTypeNameEn;

    private String vehicleTypeNameFr;
}