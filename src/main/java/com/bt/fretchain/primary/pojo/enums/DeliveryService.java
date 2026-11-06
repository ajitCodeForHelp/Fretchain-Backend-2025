package com.bt.fretchain.primary.pojo.enums;

/**
 * What a vehicle category may be used for. Freight orders (container, break bulk, parcel,
 * bulk material) are matched by the vehicle type the customer picks; shop and food orders have
 * no vehicle choice, so they are served only by categories that declare SHOP_DELIVERY / FOOD_DELIVERY.
 */
public enum DeliveryService {
    FREIGHT,
    SHOP_DELIVERY,
    FOOD_DELIVERY
}
