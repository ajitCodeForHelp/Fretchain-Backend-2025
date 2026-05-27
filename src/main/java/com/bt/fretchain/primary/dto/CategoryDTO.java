package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

public class CategoryDTO {

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
    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String nameEn;

        private String nameFr;

        private String descriptionEn;

        private String descriptionFr;

        private String imageUrl;

        private List<SubCategoryDTO.DetailForCategory> subCategories;
    }

    @Getter
    @Setter
    public static class DetailForSubCat  {
        private String uuid;

        private String nameEn;

        private String nameFr;

        private String descriptionEn;

        private String descriptionFr;

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