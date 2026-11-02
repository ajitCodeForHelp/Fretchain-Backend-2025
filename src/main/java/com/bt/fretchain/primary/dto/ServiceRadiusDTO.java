package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class ServiceRadiusDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "Area name is required")
        private String areaName;

        private String cityName;

        private String countryName;

        @NotNull(message = "Center latitude is required")
        private Double centerLatitude;

        @NotNull(message = "Center longitude is required")
        private Double centerLongitude;

        @NotNull(message = "Radius is required")
        private Long radiusInMeter;
    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String areaName;

        private String cityName;

        private String countryName;

        private Double centerLatitude;

        private Double centerLongitude;

        private Long radiusInMeter;
    }

    @Getter
    @Setter
    public static class ListOrSearchData extends BasePageRequest {

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
}