package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.dto.CustomerRestaurantDTO.RestaurantSortBy;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Input for {@link RestaurantMasterRepositoryCustom#findNearbyRestaurants}.
 *
 * @param searchPattern already lower-cased and wrapped in % (LIKE pattern), or null
 * @param now           current local time, used for the "open now" calculation
 */
public record NearbyRestaurantCriteria(
        double latitude,
        double longitude,
        double radiusKm,
        String globalCategoryUuid,
        String searchPattern,
        boolean openNowOnly,
        boolean vegOnly,
        BigDecimal minRating,
        BigDecimal maxCostForTwo,
        RestaurantSortBy sortBy,
        LocalTime now,
        int offset,
        int limit
) {
}
