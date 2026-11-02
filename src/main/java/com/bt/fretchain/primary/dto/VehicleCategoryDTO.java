package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.Category;
import com.bt.fretchain.primary.pojo.SubCategory;
import com.bt.fretchain.primary.pojo.VehicleType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class VehicleCategoryDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "nameEn is mandatory")
        @Size(max = 150)
        private String nameEn;

        @NotBlank(message = "nameFr is mandatory")
        @Size(max = 150)
        private String nameFr;

        @Size(max = 500)
        private String iconUrl;

        @NotBlank(message = "vehicleTypeUuid is mandatory")
        private String vehicleTypeUuid;

        private String categoryUuid;

        private String subCategoryUuid;

        private Long lengthCm;

        private Long widthCm;

        private Long heightCm;

        private Long maxWeightKg;

        private BigDecimal baseCharges;

        private BigDecimal baseFare;

        private Double baseKm;

        private BigDecimal perKmFare;

        private BigDecimal multiDropCharges;

        private Boolean specialHelpAvailable = false;

        private Boolean cashServices = false;

        private Integer sortOrder;

    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String nameEn;
        private String nameFr;

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

        private Integer sortOrder;
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
        private String vehicleTypeUuid;
        private String categoryUuid;
        private String subCategoryUuid;
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


    @Getter
    @Setter
    public static class Basic {

        protected String uuid;

        private String nameEn;
        private String nameFr;

        private String iconUrl;

        private Integer sortOrder;
    }

}