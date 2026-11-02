package com.bt.fretchain.primary.repository;

import java.util.List;

public interface ShopMasterRepositoryCustom {

    /**
     * Approved + active stores of the given business type that are within
     * {@code radiusKm} of the customer AND whose own delivery radius covers the
     * customer. Ordered by the requested sort, open stores always first.
     */
    List<NearbyShopRow> findNearbyShops(NearbyShopCriteria criteria);

    long countNearbyShops(NearbyShopCriteria criteria);
}
