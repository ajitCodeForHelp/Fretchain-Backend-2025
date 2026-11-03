package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** DTOs for shop logins: session, own profile, roles and staff. */
public class ShopPartnerDTO {

    /** Returned at login and by /partner/api/shop-profile/session. */
    @Getter
    @Setter
    public static class Session {
        private String storeType = "SHOP";
        private String storeUuid;
        private String storeName;
        private String storeNameFr;
        private String logoUrl;
        private ShopStatus status;
        private String roleName;
        private boolean owner;
        private List<ShopActivity> activities;
    }

    /** Fields a shop may edit itself. Status, commission, code and global categories stay admin-only. */
    @Getter
    @Setter
    public static class ProfileUpdate {
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
    }

    // ---------------------------------------------------------------- roles

    @Getter
    @Setter
    public static class RoleSaveOrUpdate {
        @NotBlank
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        private Set<ShopActivity> activities = new LinkedHashSet<>();
    }

    @Getter
    @Setter
    public static class RoleDetail {
        private String uuid;
        private String name;
        private String description;
        private boolean ownerRole;
        private List<ShopActivity> activities;
        private Boolean active;
    }

    @Getter
    @Setter
    public static class ActivityOption {
        private ShopActivity code;
        private String group;
        private boolean write;
    }

    // ---------------------------------------------------------------- staff

    @Getter
    @Setter
    public static class StaffSaveOrUpdate {
        @NotBlank
        @Size(max = 150)
        private String fullName;

        @NotBlank
        @Email
        @Size(max = 128)
        private String email;

        @Size(max = 15)
        private String mobile;

        /** Required when creating; optional on update (blank = keep current). */
        @Size(min = 6, max = 60)
        private String password;

        @NotBlank
        private String roleUuid;
    }

    /** Admin: create or update the owner login of a shop. */
    @Getter
    @Setter
    public static class OwnerSave {
        @NotBlank
        @Size(max = 150)
        private String fullName;

        @NotBlank
        @Email
        @Size(max = 128)
        private String email;

        @Size(max = 15)
        private String mobile;

        /** Required when creating; optional on update (blank = keep current). */
        @Size(min = 6, max = 60)
        private String password;
    }

    @Getter
    @Setter
    public static class StaffDetail {
        private String uuid;
        private String fullName;
        private String email;
        private String mobile;
        private String roleUuid;
        private String roleName;
        private boolean owner;
        private Boolean active;
        private LocalDateTime lastLogin;
    }

    @Getter
    @Setter
    public static class ListOrSearchData {
        private Integer pageNumber = 0;
        private Integer pageSize = 10;
        private String search;
        private String roleUuid;
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
    }
}
