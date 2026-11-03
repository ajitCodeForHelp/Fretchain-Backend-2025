package com.bt.fretchain.primary.dto;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Customer side DTOs for the Shop module (/customer/api/shop/...). */
public class CustomerShopDTO {

    // =====================================================================
    // Sorting options
    // =====================================================================

    public enum ShopSortBy {
        /** Open shops first, then nearest. Default. */
        RELEVANCE,
        DISTANCE,
        RATING,
        DELIVERY_TIME
    }

    public enum ItemSortBy {
        /** Featured first, then the shop's own display order. Default. */
        RELEVANCE,
        PRICE_LOW_TO_HIGH,
        PRICE_HIGH_TO_LOW,
        NAME
    }

    // =====================================================================
    // Requests
    // =====================================================================

    /** Common location part of customer requests. */
    @Getter
    @Setter
    public static class LocationRequest {

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private Double latitude;

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private Double longitude;

        /** Search radius. Defaults to 10 km, capped at 50 km. */
        @DecimalMin("0.5")
        @DecimalMax("50.0")
        private Double radiusKm = 10.0;
    }

    /** Nearby shops, optionally filtered by a global category. */
    @Getter
    @Setter
    public static class NearbyShopRequest extends LocationRequest {

        /** Filter: only shops assigned to this global category (chip on home screen). */
        private String globalCategoryUuid;

        /** Filter: shop name or global category name contains this text. */
        @Size(max = 100)
        private String search;

        private Boolean openNowOnly = false;

        @DecimalMin("0.0")
        @DecimalMax("5.0")
        private BigDecimal minRating;

        private ShopSortBy sortBy = ShopSortBy.RELEVANCE;

        @Min(0)
        private Integer pageNumber = 0;

        @Min(1)
        @Max(50)
        private Integer pageSize = 20;
    }

    /** Items of one shop, filtered by its category / subcategory. */
    @Getter
    @Setter
    public static class ShopItemsRequest {

        private String categoryUuid;
        private String subCategoryUuid;

        @Size(max = 100)
        private String search;

        private ItemSortBy sortBy = ItemSortBy.RELEVANCE;

        @Min(0)
        private Integer pageNumber = 0;

        @Min(1)
        @Max(100)
        private Integer pageSize = 20;
    }

    /** Global search box: "milk", "paracetamol", "Fresh Mart", "pharmacy"... */
    @Getter
    @Setter
    public static class SearchRequest extends LocationRequest {

        @NotBlank
        @Size(min = 2, max = 100)
        private String query;

        /** Max shops returned in the "shops" section. */
        @Min(1)
        @Max(50)
        private Integer shopLimit = 10;

        /** Paging applies to the "items" section. */
        @Min(0)
        private Integer pageNumber = 0;

        @Min(1)
        @Max(50)
        private Integer pageSize = 20;
    }

    // =====================================================================
    // Responses
    // =====================================================================

    @Getter
    @Setter
    public static class GlobalCategoryCard {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class ShopCard {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String logoUrl;
        private String bannerUrl;
        private String address;
        private String city;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private List<KeyValueDto> globalCategories;

        private Double distanceKm;
        private Integer estimatedDeliveryMinutes;

        private Boolean openNow;
        private Boolean acceptingOrders;
        private LocalTime openingTime;
        private LocalTime closingTime;

        private BigDecimal minOrderAmount;
        private BigDecimal deliveryFee;

        private BigDecimal rating;
        private Integer ratingCount;
    }

    @Getter
    @Setter
    public static class ItemCard {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String descriptionEn;
        private String descriptionFr;
        private String imageUrl;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private String unit;
        private String brand;
        private Boolean featured;
        private Boolean inStock;

        private String categoryUuid;
        private String categoryName;
        private String subCategoryUuid;
        private String subCategoryName;

        // Filled for cross-shop search results
        private String shopUuid;
        private String shopName;
        private Double distanceKm;
    }

    @Getter
    @Setter
    public static class ShopSubCategoryNode {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String imageUrl;
        private Long itemCount;
    }

    @Getter
    @Setter
    public static class ShopCategoryNode {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String imageUrl;
        private String globalCategoryUuid;
        private Long itemCount;
        private List<ShopSubCategoryNode> subCategories = new ArrayList<>();
    }

    /** Shop page: header + category tree (side bar / tabs). */
    @Getter
    @Setter
    public static class ShopDetailResponse {
        private ShopCard shop;
        private List<ShopCategoryNode> categories;
    }

    @Getter
    @Setter
    public static class SearchResponse {
        /** Global categories whose name matches (tap -> nearby list filtered by it). */
        private List<GlobalCategoryCard> categories;
        /** Nearby shops whose name or global category matches. */
        private List<ShopCard> shops;
        /** Matching items from nearby shops, nearest shop first. */
        private DataTableResponsePacket<ItemCard> items;
    }
}
