package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.DriverDocument;
import com.bt.fretchain.primary.pojo.enums.GenderTypeEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DriverDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Mobile is required")
        private String mobile;

        private String countryCode;

        private String email;

        private String username;

        private String password;

        private String profileImageUrl;

        private LocalDate dateOfBirth;

        private GenderTypeEnum gender;

        private String licenseNumber;

        private LocalDate licenseExpiryDate;

        private String nationalId;

        private String nationalIdNumber;
        // Driver document fields
        /** Fleet owner (vendor login uuid). Vendor panel: always the logged-in vendor. */
        private String vendorUuid;

        /** Vehicle the driver drives; must belong to the same vendor and have no other driver. */
        private String vehicleUuid;

        private DriverDocumentDetail driverDocument;

    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String fullName;

        private String mobile;

        private String countryCode;

        private String email;

        private String username;

        private String profileImageUrl;

        private LocalDate dateOfBirth;

        private GenderTypeEnum gender;

        private String licenseNumber;

        private LocalDate licenseExpiryDate;

        private String nationalIdNumber;

        private Boolean verified;

        private Boolean available;

        private Boolean onlineStatus;

        private BigDecimal overallRating;

        private BigDecimal walletBalance;

        private Long bonusPoints;

        private Double currentLat;

        private Double currentLng;

        private LocalDateTime joiningDate;

        private VehicleDTO.Basic vehicle;

        private String vendorUuid;

        private String vendorName;

        private DriverDocumentDetail driverDocument;

    }

    @Getter
    @Setter
    public static class DriverDocumentDetail {

        private String identityNumber;

        private String identityDocumentUrl;

        private String licenseImageUrl;

        private String policeVerificationUrl;

        private String addressProofUrl;

        private String contractDocumentUrl;

        private String medicalCertificateUrl;

        private LocalDate licenseValidity;

        private String referenceName;

        private String referenceMobile;
    }

    @Getter
    @Setter
    public static class ListOrSearchData extends BasePageRequest {

        private Filters filters;

        private SortRequest sort;
    }

    @Getter
    @Setter
    public static class Filters {

        private Boolean active;

        private Boolean verified;

        private Boolean available;

        /** Legacy name of vehicleUuid. */
        private String truckUuid;

        private String vehicleUuid;

        private String vendorUuid;
    }

    @Getter
    @Setter
    public static class SortRequest {

        private String field;

        private String direction;
    }
}