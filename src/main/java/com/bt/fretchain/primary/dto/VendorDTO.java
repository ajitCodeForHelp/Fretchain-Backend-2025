package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import com.bt.fretchain.primary.pojo.enums.VendorStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vendor / fleet owner: company details + its VENDOR login. */
public class VendorDTO {

    /** Company details (admin form and the vendor's own "My Company" page). */
    @Getter
    @Setter
    public static class Profile {
        @Size(max = 50)
        private String vendorCode;

        @NotBlank
        @Size(max = 150)
        private String companyName;

        @Size(max = 150)
        private String contactPerson;

        @Size(max = 20)
        private String countryCode;

        @Size(max = 20)
        private String mobile;

        @Email
        @Size(max = 150)
        private String email;

        @Size(max = 500)
        private String address;
        @Size(max = 100)
        private String city;
        @Size(max = 100)
        private String state;
        @Size(max = 100)
        private String country;
        @Size(max = 20)
        private String postalCode;

        @Size(max = 50)
        private String taxNumber;

        @Size(max = 50)
        private String registrationNumber;

        private String logoUrl;
    }

    /** Admin create / update: company details + login (password required on create only). */
    @Getter
    @Setter
    public static class SaveOrUpdate extends Profile {

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        private BigDecimal commissionPercentage;

        @NotBlank
        @Email
        @Size(max = 128)
        private String loginEmail;

        @Size(max = 15)
        private String loginMobile;

        /** Required when creating; blank on update keeps the password. */
        @Size(min = 6, max = 60)
        private String password;
    }

    /** Public sign-up: company details + login. The vendor starts PENDING until the admin approves. */
    @Getter
    @Setter
    public static class Register extends Profile {
        @NotBlank
        @Email
        @Size(max = 128)
        private String loginEmail;

        @Size(max = 15)
        private String loginMobile;

        @NotBlank
        @Size(min = 6, max = 60)
        private String password;
    }

    @Getter
    @Setter
    public static class ApprovalUpdate {
        @NotNull
        private VendorStatus status;

        /** Required when rejecting or suspending. */
        @Size(max = 500)
        private String reason;
    }

    @Getter
    @Setter
    public static class Detail {
        private String uuid;
        /** Uuid of the vendor login: used as vendorUuid on vehicles and drivers. */
        private String userUuid;
        private String vendorCode;
        private String companyName;
        private String contactPerson;
        private String countryCode;
        private String mobile;
        private String email;
        private String address;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private String taxNumber;
        private String registrationNumber;
        private String logoUrl;
        private BigDecimal commissionPercentage;
        private String loginEmail;
        private String loginMobile;
        private LocalDateTime lastLogin;
        private Long vehicleCount;
        private Long driverCount;
        private VendorStatus status;
        private String statusReason;
        private Boolean selfRegistered;
        private Boolean active;
        private LocalDateTime createdAt;
    }

    /** What the vendor panel needs about the logged-in vendor (in the session). */
    @Getter
    @Setter
    public static class Session {
        private String vendorUuid;
        private String userUuid;
        private VendorStatus status;
        private String statusReason;
        private String companyName;
        private String logoUrl;
        private Long vehicleCount;
        private Long driverCount;
    }

    @Getter
    @Setter
    public static class ChangePassword {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 6, max = 60)
        private String newPassword;
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
        private String city;
        private VendorStatus status;
    }
}
