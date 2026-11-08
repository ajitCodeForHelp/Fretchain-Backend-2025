package com.bt.fretchain.primary.pojo.enums;

public enum FreightOrderType {
    CONTAINER,
    BREAK_BULK,
    PARCEL,
    BULK_MATERIAL,
    /** Shop delivery: handled by the store order flow, not the freight flow. */
    SHOP,
    /** Food delivery (restaurants). */
    FOOD
}