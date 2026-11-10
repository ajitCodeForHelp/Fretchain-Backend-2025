package com.bt.fretchain.primary.pojo.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life cycle of a shop / food order:
 *
 *   PLACED -> ACCEPTED -> PREPARING -> READY -> OUT_FOR_DELIVERY -> DELIVERED
 *   PLACED / ACCEPTED -> REJECTED (store)      PLACED -> CANCELLED (customer)
 */
public enum StoreOrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    REJECTED,
    CANCELLED;

    /** Orders that are still in progress. */
    public static final Set<StoreOrderStatus> ACTIVE =
            EnumSet.of(PLACED, ACCEPTED, PREPARING, READY, OUT_FOR_DELIVERY);

    /** Statuses the store can move this order to. */
    public Set<StoreOrderStatus> nextForStore() {
        return switch (this) {
            case PLACED -> EnumSet.of(ACCEPTED, REJECTED);
            case ACCEPTED -> EnumSet.of(PREPARING, REJECTED);
            case PREPARING -> EnumSet.of(READY);
            case READY -> EnumSet.of(OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> EnumSet.of(DELIVERED);
            default -> EnumSet.noneOf(StoreOrderStatus.class);
        };
    }

    /** Statuses a platform admin can move this order to: the store's steps, plus cancelling an order in progress. */
    public Set<StoreOrderStatus> nextForAdmin() {
        Set<StoreOrderStatus> next = EnumSet.noneOf(StoreOrderStatus.class);
        next.addAll(nextForStore());
        if (isActive()) {
            next.add(CANCELLED);
        }
        return next;
    }

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
