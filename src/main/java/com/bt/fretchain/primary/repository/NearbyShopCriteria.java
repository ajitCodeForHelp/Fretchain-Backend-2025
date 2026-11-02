package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.dto.CustomerMarketplaceDTO.ShopSortBy;
import com.bt.fretchain.primary.pojo.enums.BusinessType;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Input for {@link ShopMasterRepositoryCustom#findNearbyShops}.
 *
 * @param searchPattern already lower-cased and wrapped in % (LIKE pattern), or null
 * @param now           current local time, used for the "open now" calculation
 */
public record NearbyShopCriteria(
        BusinessType businessType,
        double latitude,
        double longitude,
        double radiusKm,
        String globalCategoryUuid,
        String searchPattern,
        boolean openNowOnly,
        boolean vegOnly,
        BigDecimal minRating,
        ShopSortBy sortBy,
        LocalTime now,
        int offset,
        int limit
) {
}
