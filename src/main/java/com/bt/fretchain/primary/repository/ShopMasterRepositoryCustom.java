package com.bt.fretchain.primary.repository;

import java.util.List;

public interface ShopMasterRepositoryCustom {

    /**
     * Approved + active shops within {@code radiusKm} of the customer AND whose
     * own delivery radius covers the customer. Ordered by the requested sort,
     * open shops always first.
     */
    List<NearbyShopRow> findNearbyShops(NearbyShopCriteria criteria);

    long countNearbyShops(NearbyShopCriteria criteria);
}
