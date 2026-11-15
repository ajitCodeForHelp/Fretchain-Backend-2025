package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.VehicleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class VehicleDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "vehicleNumber is mandatory")
        @Size(max = 50)
        private String vehicleNumber;

        private String rcNumber;

        private String fitnessCertificateNumber;

        private String chassisNumber;

        private Long maxWeightKg;

        private Boolean verified = false;

        @NotBlank(message = "vehicleCategoryUuid is mandatory")
        private String vehicleCategoryUuid;

        private String vendorUuid;

    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String vehicleNumber;

        private String rcNumber;

        private String fitnessCertificateNumber;

        private String chassisNumber;

        private Long maxWeightKg;

        private Boolean verified;

        private com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus verificationStatus;

        private String verificationReason;

        private java.time.LocalDateTime verificationChangedAt;

        private String verificationReviewer;

        private VehicleCategoryDTO.Basic vehicleCategory;

        private String vendorUuid;
        private String vendorName;

        /** Driver currently assigned to this vehicle (if any). */
        private String driverUuid;
        private String driverName;

    }

    @Getter
    @Setter
    public static class Basic {
        protected String uuid;
        private String vehicleNumber;
        private String vehicleCategoryUuid;
        private String vehicleCategoryNameEn;
        private String vehicleCategoryNameFr;
    }

    @Getter
    @Setter
    public static class ListOrSearchData {
        private Integer pageNumber = 0;
        private Integer pageSize = 10;
        private String search;
        private Filters filters;
        private DateFilter dateFilter;
        private SortRequest sort;
    }

    @Getter
    @Setter
    public static class Filters {
        private Boolean active;
        private Boolean verified;
        /** PENDING / VERIFIED / REJECTED */
        private com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus verificationStatus;
        private String vehicleCategoryUuid;
        private String vendorUuid;
    }

    @Getter
    @Setter
    public static class DateFilter {
        private LocalDate from;
        private LocalDate to;
    }

    @Getter
    @Setter
    public static class SortRequest {
        private String field;
        private String direction;
    }

    /** Approve / reject (reason required) / send back to pending. */
    @Getter
    @Setter
    public static class Review {

        @jakarta.validation.constraints.NotNull(message = "Status is required")
        private com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus status;

        @jakarta.validation.constraints.Size(max = 500, message = "Reason is too long")
        private String reason;
    }
}
