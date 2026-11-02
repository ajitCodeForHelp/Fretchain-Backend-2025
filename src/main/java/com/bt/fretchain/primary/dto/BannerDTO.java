package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BannerDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "English title is required")
        private String titleEn;

        @NotBlank(message = "French title is required")
        private String titleFr;

        private String descriptionEn;

        private String descriptionFr;

        @NotBlank(message = "Image is required")
        private String imageUrl;

        private String redirectUrl;

        private Integer sortOrder;

        private LocalDateTime startDate;

        private LocalDateTime endDate;
    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String titleEn;

        private String titleFr;

        private String descriptionEn;

        private String descriptionFr;

        private String imageUrl;

        private String redirectUrl;

        private Integer sortOrder;

        private LocalDateTime startDate;

        private LocalDateTime endDate;
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