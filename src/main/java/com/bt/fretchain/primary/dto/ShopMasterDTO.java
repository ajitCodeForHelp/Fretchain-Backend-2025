package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

public class ShopMasterDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @Size(max = 50)
        private String shopCode;

        @NotBlank
        @Size(max = 150)
        private String nameEn;

        @Size(max = 150)
        private String nameFr;

        private String descriptionEn;
        private String descriptionFr;

        @Size(max = 20)
        private String countryCode;

        @Size(max = 20)
        private String mobile;

        @Email
        @Size(max = 150)
        private String email;

        private String logoUrl;
        private String bannerUrl;
        private String address;
        private String city;
        private String state;
        private String country;
        private String postalCode;

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private BigDecimal latitude;

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private BigDecimal longitude;

        private LocalTime openingTime;
        private LocalTime closingTime;

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        private BigDecimal commissionPercentage;

        private String status;

        // UUID of the shop owner, if assigned
        private String ownerUuid;
    }

    @Getter
    @Setter
    public static class Detail {
        private String uuid;
        private String shopCode;
        private String nameEn;
        private String nameFr;
        private String descriptionEn;
        private String descriptionFr;
        private String countryCode;
        private String mobile;
        private String email;
        private String logoUrl;
        private String bannerUrl;
        private String address;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalTime openingTime;
        private LocalTime closingTime;
        private BigDecimal commissionPercentage;
        private String status;
        private String ownerUuid;
        private String ownerName;
        private Boolean active;
        private Boolean deleted;
    }

    @Getter
    @Setter
    public static class ListOrSearchData {
        private Integer pageNumber = 0;
        private Integer pageSize = 10;
        private String search;
        private SortData sort;
        private FilterData filters;
    }

    @Getter
    @Setter
    public static class SortData {
        private String field;
        private String direction;
    }

    @Getter
    @Setter
    public static class FilterData {
        private Boolean active;
        private String status;
    }
}