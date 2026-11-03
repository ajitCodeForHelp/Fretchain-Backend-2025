package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class RestaurantCategoryDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank
        @Size(max = 150)
        private String nameEn;

        @Size(max = 150)
        private String nameFr;

        private String descriptionEn;
        private String descriptionFr;
        private String imageUrl;

        private Integer displayOrder = 0;

        @NotBlank
        private String restaurantUuid;

        /** Optional. Must be one of the restaurant's assigned global categories. */
        private String globalCategoryUuid;
    }

    @Getter
    @Setter
    public static class Detail {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String descriptionEn;
        private String descriptionFr;
        private String imageUrl;
        private Integer displayOrder;
        private String restaurantUuid;
        private String restaurantName;
        private String globalCategoryUuid;
        private String globalCategoryName;
        private Boolean active;
        private Boolean deleted;
    }

    @Getter
    @Setter
    public static class ListOrSearchData {
        private Integer pageNumber = 0;
        private Integer pageSize = 10;
        private String search;
        private String restaurantUuid;
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
    }
}