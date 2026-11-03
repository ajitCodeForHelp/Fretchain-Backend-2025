package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.FoodType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class RestaurantItemDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @Size(max = 50)
        private String itemCode;

        @NotBlank
        @Size(max = 200)
        private String nameEn;

        @Size(max = 200)
        private String nameFr;

        private String descriptionEn;
        private String descriptionFr;

        @NotNull
        @DecimalMin(value = "0.0")
        private BigDecimal price;

        @DecimalMin(value = "0.0")
        private BigDecimal discountPrice;

        /** VEG / NON_VEG / EGG. */
        private FoodType foodType;

        @Size(max = 100)
        private String servingSize;

        @Min(0)
        private Integer preparationMinutes;

        private String imageUrl;

        /** Comma separated keywords, e.g. "paneer,cottage cheese". */
        @Size(max = 500)
        private String searchTags;

        private Boolean available = true;
        private Boolean featured = false;

        private Integer displayOrder = 0;

        @NotBlank
        private String restaurantUuid;

        @NotBlank
        private String categoryUuid;

        // Optional
        private String subCategoryUuid;
    }

    @Getter
    @Setter
    public static class Detail {
        private String uuid;
        private String itemCode;
        private String nameEn;
        private String nameFr;
        private String descriptionEn;
        private String descriptionFr;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private FoodType foodType;
        private String servingSize;
        private Integer preparationMinutes;
        private String imageUrl;
        private String searchTags;
        private Boolean available;
        private Boolean featured;
        private Integer displayOrder;
        private String restaurantUuid;
        private String restaurantName;
        private String categoryUuid;
        private String categoryName;
        private String subCategoryUuid;
        private String subCategoryName;
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
        private String subCategoryUuid;
        private Boolean available;
        private FoodType foodType;
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