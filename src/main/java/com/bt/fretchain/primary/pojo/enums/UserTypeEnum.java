package com.bt.fretchain.primary.pojo.enums;

public enum UserTypeEnum {

    SUPER_ADMIN,
    ADMIN,

    CUSTOMER,

    CORPORATE_CLIENT,

    DRIVER,

    VENDOR,

    STAFF,

    /** Platform staff (accountant, order manager...): permissions come from their PlatformRole. */
    ADMIN_STAFF,

    /** Owns one or more shops (ShopMaster.owner); has every permission in each of them. */
    SHOP_OWNER,

    /** Shop admin or shop staff of ONE shop; permissions come from their ShopRole. */
    SHOP_USER,

    /** Owns one or more restaurants (RestaurantMaster.owner). */
    RESTAURANT_OWNER,

    /** Restaurant admin or staff of ONE restaurant; permissions come from their RestaurantRole. */
    RESTAURANT_USER
}