package com.bt.fretchain.primary.dto;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.pojo.enums.FoodType;
import com.bt.fretchain.primary.pojo.enums.RestaurantStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RestaurantMasterDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @Size(max = 50)
        private String restaurantCode;

        /** Cuisines to assign (at least one), e.g. ["<pizza-uuid>", "<italian-uuid>"]. */
        @NotEmpty
        private List<String> globalCategoryUuids = new ArrayList<>();

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

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private BigDecimal latitude;

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private BigDecimal longitude;

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        private BigDecimal deliveryRadiusKm;

        private LocalTime openingTime;
        private LocalTime closingTime;
        private Boolean acceptingOrders;

        @Min(0)
        private Integer avgPreparationMinutes;

        @DecimalMin("0.0")
        private BigDecimal minOrderAmount;

        @DecimalMin("0.0")
        private BigDecimal deliveryFee;

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        private BigDecimal commissionPercentage;

        /** VEG = pure veg restaurant, BOTH = veg and non-veg. */
        private FoodType foodType;

        @DecimalMin("0.0")
        private BigDecimal costForTwo;

        /** Only used on create (defaults to PENDING). Later changes go through the approval action. */
        private RestaurantStatus status;

        // ---- Owner login (required on create; later changes via /{uuid}/owner)
        /** Owner's name. Saved in system_user as a RESTAURANT_USER with the Owner role. */
        @Size(max = 150)
        private String ownerName;

        @Email
        @Size(max = 128)
        private String ownerEmail;

        @Size(max = 15)
        private String ownerMobile;

        @Size(min = 6, max = 60)
        private String ownerPassword;

        /** Instead of a new owner: an existing RESTAURANT_OWNER who already owns other restaurants. */
        private String existingOwnerUuid;
    }

    @Getter
    @Setter
    public static class Detail {
        private String uuid;
        private String restaurantCode;
        private List<KeyValueDto> globalCategories;
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
        private BigDecimal deliveryRadiusKm;
        private LocalTime openingTime;
        private LocalTime closingTime;
        private Boolean acceptingOrders;
        private Integer avgPreparationMinutes;
        private BigDecimal minOrderAmount;
        private BigDecimal deliveryFee;
        private BigDecimal commissionPercentage;
        private FoodType foodType;
        private BigDecimal costForTwo;
        private BigDecimal rating;
        private Integer ratingCount;
        private RestaurantStatus status;
        private String statusReason;
        private java.time.LocalDateTime statusChangedAt;
        private Boolean selfRegistered;
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
        private RestaurantStatus status;
        private FoodType foodType;
        private String globalCategoryUuid;
        private String city;
    }
}
