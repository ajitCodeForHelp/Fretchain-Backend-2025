package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class VehicleTypeDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "nameEn is mandatory")
        @Size(max = 120)
        private String nameEn;

        @NotBlank(message = "nameFr is mandatory")
        @Size(max = 120)
        private String nameFr;

        @Size(max = 500)
        private String iconUrl;

    }

    @Getter
    @Setter
    public static class Detail extends BasicDto{
        private String nameEn;
        private String nameFr;
        private String iconUrl;
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
    public static class BasicDetail {
        protected String uuid;

        private String nameEn;
        private String nameFr;
        private String iconUrl;
        private Integer sortOrder;
    }
}