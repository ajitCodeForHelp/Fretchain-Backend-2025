package com.bt.fretchain.primary.dto;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.pojo.enums.FoodType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Customer side DTOs for the Restaurant module (/customer/api/restaurant/...). */
public class CustomerRestaurantDTO {

    // =====================================================================
    // Sorting options
    // =====================================================================

    public enum RestaurantSortBy {
        /** Open restaurants first, then nearest. Default. */
        RELEVANCE,
        DISTANCE,
        RATING,
        DELIVERY_TIME,
        COST_LOW_TO_HIGH,
        COST_HIGH_TO_LOW
    }

    public enum DishSortBy {
        /** Featured (bestseller) first, then the restaurant's own display order. Default. */
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

    /** Nearby restaurants, optionally filtered by cuisine. */
    @Getter
    @Setter
    public static class NearbyRestaurantRequest extends LocationRequest {

        /** Filter: only restaurants serving this cuisine (chip on home screen). */
        private String globalCategoryUuid;

        /** Filter: restaurant name or cuisine name contains this text. */
        @Size(max = 100)
        private String search;

        private Boolean openNowOnly = false;

        /** Only pure-veg restaurants. */
        private Boolean vegOnly = false;

        @DecimalMin("0.0")
        @DecimalMax("5.0")
        private BigDecimal minRating;

        /** Filter: cost for two at most this amount. */
        @DecimalMin("0.0")
        private BigDecimal maxCostForTwo;

        private RestaurantSortBy sortBy = RestaurantSortBy.RELEVANCE;

        @Min(0)
        private Integer pageNumber = 0;

        @Min(1)
        @Max(50)
        private Integer pageSize = 20;
    }

    /** Dishes of one restaurant, filtered by menu section. */
    @Getter
    @Setter
    public static class DishRequest {

        private String categoryUuid;
        private String subCategoryUuid;

        @Size(max = 100)
        private String search;

        /** Only VEG dishes. */
        private Boolean vegOnly = false;

        private DishSortBy sortBy = DishSortBy.RELEVANCE;

        @Min(0)
        private Integer pageNumber = 0;

        @Min(1)
        @Max(100)
        private Integer pageSize = 20;
    }

    /** Global search box: "pizza", "paneer tikka", "Dominos", "biryani"... */
    @Getter
    @Setter
    public static class SearchRequest extends LocationRequest {

        @NotBlank
        @Size(min = 2, max = 100)
        private String query;

        private Boolean vegOnly = false;

        /** Max restaurants returned in the "restaurants" section. */
        @Min(1)
        @Max(50)
        private Integer restaurantLimit = 10;

        /** Paging applies to the "dishes" section. */
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
    public static class CuisineCard {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class RestaurantCard {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String logoUrl;
        private String bannerUrl;
        private String address;
        private String city;
        private BigDecimal latitude;
        private BigDecimal longitude;
        /** Cuisines. */
        private List<KeyValueDto> globalCategories;

        private Double distanceKm;
        private Integer estimatedDeliveryMinutes;

        private Boolean openNow;
        private Boolean acceptingOrders;
        private LocalTime openingTime;
        private LocalTime closingTime;

        private BigDecimal minOrderAmount;
        private BigDecimal deliveryFee;

        private FoodType foodType;
        private BigDecimal costForTwo;

        private BigDecimal rating;
        private Integer ratingCount;
    }

    @Getter
    @Setter
    public static class DishCard {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String descriptionEn;
        private String descriptionFr;
        private String imageUrl;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private FoodType foodType;
        private String servingSize;
        private Integer preparationMinutes;
        /** Bestseller / recommended. */
        private Boolean featured;
        private Boolean available;

        private String categoryUuid;
        private String categoryName;
        private String subCategoryUuid;
        private String subCategoryName;

        // Filled for cross-restaurant search results
        private String restaurantUuid;
        private String restaurantName;
        private Double distanceKm;
    }

    @Getter
    @Setter
    public static class MenuSubCategory {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String imageUrl;
        private Long itemCount;
        /** Filled only by the full-menu endpoint. */
        private List<DishCard> items;
    }

    @Getter
    @Setter
    public static class MenuCategory {
        private String uuid;
        private String nameEn;
        private String nameFr;
        private String imageUrl;
        private String globalCategoryUuid;
        private Long itemCount;
        private List<MenuSubCategory> subCategories = new ArrayList<>();
        /** Filled only by the full-menu endpoint (dishes with no subcategory). */
        private List<DishCard> items;
    }

    /** Restaurant page: header + menu sections (optionally with dishes). */
    @Getter
    @Setter
    public static class RestaurantDetailResponse {
        private RestaurantCard restaurant;
        private List<MenuCategory> categories;
    }

    @Getter
    @Setter
    public static class SearchResponse {
        /** Cuisines whose name matches (tap -> nearby list filtered by it). */
        private List<CuisineCard> cuisines;
        /** Nearby restaurants whose name or cuisine matches. */
        private List<RestaurantCard> restaurants;
        /** Matching dishes from nearby restaurants, nearest restaurant first. */
        private DataTableResponsePacket<DishCard> dishes;
    }
}
