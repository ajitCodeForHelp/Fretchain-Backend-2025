package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class SubCategoryDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "English Name is required")
        private String nameEn;

        @NotBlank(message = "French Name is required")
        private String nameFr;

        private String descriptionEn;

        private String descriptionFr;

        private String imageUrl;

        @NotNull(message = "Category is required")
        private String categoryUuid;
    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String nameEn;

        private String nameFr;

        private String descriptionEn;

        private String descriptionFr;

        private String imageUrl;

        private CategoryDTO.DetailForSubCat category;
    }

    @Getter
    @Setter
    public static class DetailForCategory {

        private String uuid;

        private String nameEn;

        private String nameFr;

        private String imageUrl;
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

        private String categoryUuid;
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