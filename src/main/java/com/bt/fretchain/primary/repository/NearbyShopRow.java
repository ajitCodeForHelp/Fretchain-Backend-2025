package com.bt.fretchain.primary.repository;

/** One row of the nearby query: the shop id plus computed values. */
public record NearbyShopRow(
        Long shopId,
        double distanceKm,
        boolean openNow
) {
}
