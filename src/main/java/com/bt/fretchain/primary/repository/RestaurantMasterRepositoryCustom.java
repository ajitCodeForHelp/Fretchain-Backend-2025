package com.bt.fretchain.primary.repository;

import java.util.List;

public interface RestaurantMasterRepositoryCustom {

    /**
     * Approved + active restaurants within {@code radiusKm} of the customer AND whose
     * own delivery radius covers the customer. Ordered by the requested sort,
     * open restaurants always first.
     */
    List<NearbyRestaurantRow> findNearbyRestaurants(NearbyRestaurantCriteria criteria);

    long countNearbyRestaurants(NearbyRestaurantCriteria criteria);
}
