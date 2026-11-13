package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.AssignmentMode;
import com.bt.fretchain.primary.pojo.enums.DeliveryService;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Driver dispatch for shop / food orders (no vehicle type is chosen by the customer). */
public class DispatchDTO {

    @Getter
    @Setter
    public static class Settings {
        @NotNull
        private AssignmentMode mode;

        @NotNull
        @DecimalMin("0.5")
        @DecimalMax("100.0")
        private Double radiusKm;

        // ---- freight orders and offer timing (optional on update: missing = keep the current value)
        private AssignmentMode freightMode;

        @DecimalMin("1.0")
        @DecimalMax("500.0")
        private Double freightRadiusKm;

        /** Seconds a driver has to answer a shop / food offer. */
        @jakarta.validation.constraints.Min(10)
        @jakarta.validation.constraints.Max(300)
        private Integer storeOfferSeconds;

        /** Seconds drivers have to answer a freight offer. */
        @jakarta.validation.constraints.Min(30)
        @jakarta.validation.constraints.Max(1800)
        private Integer freightOfferSeconds;

        /** Nearest drivers asked at the same time for a freight order. */
        @jakarta.validation.constraints.Min(1)
        @jakarta.validation.constraints.Max(10)
        private Integer freightWaveSize;
    }

    @Getter
    @Setter
    public static class Candidate {
        private String driverUuid;
        private String fullName;
        private String mobile;
        private String countryCode;
        private String profileImageUrl;
        /** Null = driver of the Fretchain platform. */
        private String vendorName;
        private String vehicleNumber;
        private String vehicleCategory;
        private Double distanceKm;
        private java.math.BigDecimal rating;
        private LocalDateTime lastLocationUpdatedAt;
    }

    /** Why drivers are (not) ready for a service: counts per blocking reason. */
    @Getter
    @Setter
    public static class Readiness {
        private DeliveryService service;
        private long totalDrivers;
        private long ready;
        /** reason key -> number of drivers blocked (first failing check only). */
        private Map<String, Long> blockedBy;
    }

    @Getter
    @Setter
    public static class Overview {
        private Settings settings;
        private List<Readiness> readiness;
        /** category name -> services it serves */
        private Map<String, List<DeliveryService>> categories;
    }
}
