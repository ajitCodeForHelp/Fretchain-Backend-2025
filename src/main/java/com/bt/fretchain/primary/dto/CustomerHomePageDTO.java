package com.bt.fretchain.primary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class CustomerHomePageDTO {

    @Getter
    @Setter
    public static class BannerResponse {

        private String uuid;

        private String titleEn;

        private String titleFr;

        private String descriptionEn;

        private String descriptionFr;

        private String imageUrl;

        private String redirectUrl;
    }

    @Getter
    @Setter
    public static class CategoryResponse {

        private String uuid;

        private String nameEn;
        private String nameFr;

        private String descriptionEn;
        private String descriptionFr;

        private String imageUrl;

        private List<SubCategoryResponse> subCategories;
    }

    @Getter
    @Setter
    public static class SubCategoryResponse {

        private String uuid;

        private String nameEn;
        private String nameFr;

        private String descriptionEn;
        private String descriptionFr;

        private String imageUrl;
    }


    @Getter
    @Setter
    public static class VehicleCategoryFareRequest {

        private String categoryUuid;

        private String subCategoryUuid;

        private Double distanceKm;

        private Integer estimatedTimeMinutes;

        private Integer dropCount;
    }

    @Getter
    @Setter
    public static class VehicleCategoryResponse {

        private String uuid;

        private String nameEn;
        private String nameFr;

        private String subTitleEn;
        private String subTitleFr;

        private String iconUrl;

        private VehicleTypeDTO.BasicDetail vehicleType;

        private CategoryDTO.DetailForSubCat category;

        private SubCategoryDTO.DetailForCategory subCategory;

        private Long lengthCm;
        private Long widthCm;
        private Long heightCm;
        private Long maxWeightKg;

        private BigDecimal baseCharges;
        private BigDecimal baseFare;
        private Double baseKm;
        private BigDecimal perKmFare;
        private BigDecimal multiDropCharges;

        private Boolean specialHelpAvailable;
        private Boolean cashServices;

        // customer side calculated fields
        private Double distanceKm;
        private Integer estimatedTimeMinutes;
        private Integer dropCount;
        private BigDecimal estimatedFare;
    }


    @Getter
    @Setter
    public static class CheckServiceRequest {

        @NotNull(message = "Pickup latitude is required")
        private Double pickupLatitude;

        @NotNull(message = "Pickup longitude is required")
        private Double pickupLongitude;

        @Valid
        @NotEmpty(message = "At least one drop location is required")
        private List<DropLocationRequest> dropLocations;
    }

    @Getter
    @Setter
    public static class DropLocationRequest {

        @NotNull(message = "Drop latitude is required")
        private Double latitude;

        @NotNull(message = "Drop longitude is required")
        private Double longitude;
    }

    @Getter
    @Setter
    public static class CheckServiceResponse {

        private Boolean serviceAvailable;

        private Boolean pickupServiceAvailable;

        private Boolean allDropServiceAvailable;

        private Integer dropCount;

        private Double distanceKm;

        private Integer estimatedTimeMinutes;

        private String message;
    }
}