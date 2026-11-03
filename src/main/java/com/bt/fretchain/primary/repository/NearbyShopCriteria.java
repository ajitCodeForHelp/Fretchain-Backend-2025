package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.dto.CustomerShopDTO.ShopSortBy;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Input for {@link ShopMasterRepositoryCustom#findNearbyShops}.
 *
 * @param searchPattern already lower-cased and wrapped in % (LIKE pattern), or null
 * @param now           current local time, used for the "open now" calculation
 */
public record NearbyShopCriteria(
        double latitude,
        double longitude,
        double radiusKm,
        String globalCategoryUuid,
        String searchPattern,
        boolean openNowOnly,
        BigDecimal minRating,
        ShopSortBy sortBy,
        LocalTime now,
        int offset,
        int limit
) {
}
