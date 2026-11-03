package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Public shop registration and the admin approval action. */
public class ShopRegistrationDTO {

    /** Filled by the shop itself on the public page. Approval fields are never accepted here. */
    @Getter
    @Setter
    public static class Register {

        // ---- shop
        @NotBlank
        @Size(max = 150)
        private String nameEn;

        @Size(max = 150)
        private String nameFr;

        private String descriptionEn;
        private String descriptionFr;

        @NotEmpty
        private List<String> globalCategoryUuids = new ArrayList<>();

        @Size(max = 20)
        private String countryCode;

        @NotBlank
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

        private LocalTime openingTime;
        private LocalTime closingTime;

        // ---- owner login
        @NotBlank
        @Size(max = 150)
        private String ownerName;

        @NotBlank
        @Email
        @Size(max = 128)
        private String ownerEmail;

        @Size(max = 15)
        private String ownerMobile;

        @NotBlank
        @Size(min = 6, max = 60)
        private String password;
    }

    @Getter
    @Setter
    public static class Registered {
        private String shopUuid;
        private String shopName;
        private ShopStatus status;
        private String loginEmail;
    }

    /** Admin: approve / reject / suspend. Reason is required for REJECTED and SUSPENDED. */
    @Getter
    @Setter
    public static class ApprovalUpdate {
        @NotNull
        private ShopStatus status;

        @Size(max = 500)
        private String reason;
    }
}
