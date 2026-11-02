package com.bt.fretchain.primary.pojo.enums;

import com.bt.fretchain.exception.BadRequestException;

/**
 * Type of store in the marketplace. Shops (grocery, pharmacy, electronics...)
 * and restaurants share the same tables: store -> category -> subcategory -> item.
 */
public enum BusinessType {
    SHOP,
    RESTAURANT;

    /** Resolves a URL path segment such as "shop" or "restaurant". */
    public static BusinessType fromPath(String value) {
        if (value != null) {
            for (BusinessType type : values()) {
                if (type.name().equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
        }
        throw new BadRequestException("business.type.invalid");
    }
}
