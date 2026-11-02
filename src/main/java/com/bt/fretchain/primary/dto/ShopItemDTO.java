package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class ShopItemDTO {

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

        @Size(max = 100)
        private String sku;

        @NotNull
        @DecimalMin(value = "0.0")
        private BigDecimal price;

        @DecimalMin(value = "0.0")
        private BigDecimal discountPrice;

        @NotNull
        @Min(0)
        private Integer stockQuantity = 0;

        private String unit;
        private String brand;

        @DecimalMin(value = "0.0")
        private BigDecimal weightKg;

        private String imageUrl;

        private Boolean available = true;
        private Boolean featured = false;

        private Integer displayOrder = 0;

        @NotBlank
        private String shopUuid;

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
        private String sku;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private Integer stockQuantity;
        private String unit;
        private String brand;
        private BigDecimal weightKg;
        private String imageUrl;
        private Boolean available;
        private Boolean featured;
        private Integer displayOrder;
        private String shopUuid;
        private String shopName;
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
        private String shopUuid;
        private String categoryUuid;
        private String subCategoryUuid;
        private Boolean available;
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