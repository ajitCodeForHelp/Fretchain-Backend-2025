package com.bt.fretchain.primary.pojo.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Permissions for platform admin staff (ADMIN_STAFF): one admin-panel menu x one action.
 * SUPER_ADMIN and ADMIN always have everything.
 *
 * Each menu lists the /admin/api resources it covers; PanelAuthorizationInterceptor maps
 * the request to (menu, action):  GET / list -> VIEW, POST /save -> ADD, PUT -> EDIT,
 * PATCH (status, approval) -> STATUS, other POST -> EDIT.
 */
public enum PlatformActivity {

    // menu-group                    resources (path segment after /admin/api/)
    DASHBOARD_VIEW(Menu.DASHBOARD, Action.VIEW),
    ORDER_VIEW(Menu.ORDER, Action.VIEW), ORDER_ADD(Menu.ORDER, Action.ADD), ORDER_EDIT(Menu.ORDER, Action.EDIT), ORDER_STATUS(Menu.ORDER, Action.STATUS),
    BANNER_VIEW(Menu.BANNER, Action.VIEW), BANNER_ADD(Menu.BANNER, Action.ADD), BANNER_EDIT(Menu.BANNER, Action.EDIT), BANNER_STATUS(Menu.BANNER, Action.STATUS),
    COUPON_VIEW(Menu.COUPON, Action.VIEW), COUPON_ADD(Menu.COUPON, Action.ADD), COUPON_EDIT(Menu.COUPON, Action.EDIT), COUPON_STATUS(Menu.COUPON, Action.STATUS),
    CATEGORY_VIEW(Menu.CATEGORY, Action.VIEW), CATEGORY_ADD(Menu.CATEGORY, Action.ADD), CATEGORY_EDIT(Menu.CATEGORY, Action.EDIT), CATEGORY_STATUS(Menu.CATEGORY, Action.STATUS),
    VEHICLE_VIEW(Menu.VEHICLE, Action.VIEW), VEHICLE_ADD(Menu.VEHICLE, Action.ADD), VEHICLE_EDIT(Menu.VEHICLE, Action.EDIT), VEHICLE_STATUS(Menu.VEHICLE, Action.STATUS),
    DRIVER_VIEW(Menu.DRIVER, Action.VIEW), DRIVER_ADD(Menu.DRIVER, Action.ADD), DRIVER_EDIT(Menu.DRIVER, Action.EDIT), DRIVER_STATUS(Menu.DRIVER, Action.STATUS),
    VENDOR_VIEW(Menu.VENDOR, Action.VIEW), VENDOR_ADD(Menu.VENDOR, Action.ADD), VENDOR_EDIT(Menu.VENDOR, Action.EDIT), VENDOR_STATUS(Menu.VENDOR, Action.STATUS),
    SHOP_VIEW(Menu.SHOP, Action.VIEW), SHOP_ADD(Menu.SHOP, Action.ADD), SHOP_EDIT(Menu.SHOP, Action.EDIT), SHOP_STATUS(Menu.SHOP, Action.STATUS),
    RESTAURANT_VIEW(Menu.RESTAURANT, Action.VIEW), RESTAURANT_ADD(Menu.RESTAURANT, Action.ADD), RESTAURANT_EDIT(Menu.RESTAURANT, Action.EDIT), RESTAURANT_STATUS(Menu.RESTAURANT, Action.STATUS),
    SETTING_VIEW(Menu.SETTING, Action.VIEW), SETTING_ADD(Menu.SETTING, Action.ADD), SETTING_EDIT(Menu.SETTING, Action.EDIT), SETTING_STATUS(Menu.SETTING, Action.STATUS),
    ADMIN_ROLE_VIEW(Menu.ADMIN_ROLE, Action.VIEW), ADMIN_ROLE_ADD(Menu.ADMIN_ROLE, Action.ADD), ADMIN_ROLE_EDIT(Menu.ADMIN_ROLE, Action.EDIT), ADMIN_ROLE_STATUS(Menu.ADMIN_ROLE, Action.STATUS),
    ADMIN_STAFF_VIEW(Menu.ADMIN_STAFF, Action.VIEW), ADMIN_STAFF_ADD(Menu.ADMIN_STAFF, Action.ADD), ADMIN_STAFF_EDIT(Menu.ADMIN_STAFF, Action.EDIT), ADMIN_STAFF_STATUS(Menu.ADMIN_STAFF, Action.STATUS);

    public enum Action { VIEW, ADD, EDIT, STATUS }

    /** Admin-panel menus and the /admin/api resources each one covers. */
    public enum Menu {
        DASHBOARD(),
        ORDER("freight-order"),
        BANNER("banner"),
        COUPON("coupon"),
        CATEGORY("category", "sub-category"),
        VEHICLE("vehicle", "vehicle-type", "vehicle-category"),
        DRIVER("driver"),
        VENDOR("vendor"),
        SHOP("shop", "shop-global-category", "shop-category", "shop-subcategory", "shop-item"),
        RESTAURANT("restaurant", "restaurant-global-category", "restaurant-category", "restaurant-subcategory", "restaurant-item"),
        SETTING("settings", "service-radius"),
        ADMIN_ROLE("admin-role"),
        ADMIN_STAFF("admin-staff");

        private final List<String> resources;

        Menu(String... resources) {
            this.resources = Arrays.asList(resources);
        }

        public List<String> getResources() {
            return resources;
        }

        public static Menu forResource(String resource) {
            for (Menu m : values()) {
                if (m.resources.contains(resource)) {
                    return m;
                }
            }
            return null;
        }
    }

    private final Menu menu;
    private final Action action;

    PlatformActivity(Menu menu, Action action) {
        this.menu = menu;
        this.action = action;
    }

    public Menu getMenu() {
        return menu;
    }

    public Action getAction() {
        return action;
    }

    public static PlatformActivity of(Menu menu, Action action) {
        for (PlatformActivity a : values()) {
            if (a.menu == menu && a.action == action) {
                return a;
            }
        }
        return null;
    }
}
