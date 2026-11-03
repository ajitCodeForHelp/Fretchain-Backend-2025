package com.bt.fretchain.primary.pojo.enums;

/**
 * Activities (permissions) a shop role can be given.
 * MANAGE always includes VIEW. The owner role implicitly has every activity.
 */
public enum ShopActivity {
    PROFILE_VIEW("PROFILE"),
    PROFILE_EDIT("PROFILE"),

    CATEGORY_VIEW("CATEGORY"),
    CATEGORY_MANAGE("CATEGORY"),

    SUBCATEGORY_VIEW("SUBCATEGORY"),
    SUBCATEGORY_MANAGE("SUBCATEGORY"),

    ITEM_VIEW("ITEM"),
    ITEM_MANAGE("ITEM"),

    ROLE_VIEW("ROLE"),
    ROLE_MANAGE("ROLE"),

    STAFF_VIEW("STAFF"),
    STAFF_MANAGE("STAFF");

    private final String group;

    ShopActivity(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    /** True for *_MANAGE / *_EDIT activities. */
    public boolean isWrite() {
        return !name().endsWith("_VIEW");
    }

    /** The *_VIEW activity of the same group. */
    public ShopActivity viewActivity() {
        for (ShopActivity a : values()) {
            if (a.group.equals(group) && !a.isWrite()) {
                return a;
            }
        }
        return this;
    }
}
