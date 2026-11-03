package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class RestaurantSubCategoryDTO {

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

        @NotBlank
        private String categoryUuid;
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
        private String categoryUuid;
        private String categoryName;
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
        private String categoryUuid;
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