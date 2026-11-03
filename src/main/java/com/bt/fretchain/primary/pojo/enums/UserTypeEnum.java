package com.bt.fretchain.primary.pojo.enums;

public enum UserTypeEnum {

    SUPER_ADMIN,
    ADMIN,

    CUSTOMER,

    CORPORATE_CLIENT,

    DRIVER,

    VENDOR,

    STAFF,

    /** Login belonging to a shop (owner or staff); permissions come from ShopRole. */
    SHOP_USER,

    /** Login belonging to a restaurant (owner or staff); permissions come from RestaurantRole. */
    RESTAURANT_USER
}