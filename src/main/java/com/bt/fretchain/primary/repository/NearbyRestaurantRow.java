package com.bt.fretchain.primary.repository;

/** One row of the nearby query: the restaurant id plus computed values. */
public record NearbyRestaurantRow(
        Long restaurantId,
        double distanceKm,
        boolean openNow
) {
}
