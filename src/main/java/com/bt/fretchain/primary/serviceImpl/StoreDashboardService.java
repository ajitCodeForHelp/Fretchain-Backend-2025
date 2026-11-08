package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.DashboardDTO;
import com.bt.fretchain.primary.dto.DashboardDTO.Response;
import com.bt.fretchain.primary.dto.DashboardDTO.Section;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.RestaurantActivity;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;

import static com.bt.fretchain.primary.dto.DashboardDTO.*;

/**
 * Dashboard of a shop / restaurant login (owner, admin or staff). Widgets are limited to
 * what the login's role may open (CATEGORY_VIEW, ITEM_VIEW, STAFF_VIEW ...).
 */
@Service
@RequiredArgsConstructor
public class StoreDashboardService {

    private static final int LOW_STOCK = 5;

    private final DashboardQuery q;
    private final ShopAccessService shopAccess;
    private final RestaurantAccessService restaurantAccess;

    @Transactional(readOnly = true)
    public Response shop(SystemUser user) {
        var staff = shopAccess.currentStaff(user);
        ShopMaster s = staff.getShop();
        Predicate<String> can = a -> shopAccess.has(staff, ShopActivity.valueOf(a));
        List<Map<String, Object>> checks = List.of(
                check("logo", notBlank(s.getLogoUrl()), "/myShop"),
                check("banner", notBlank(s.getBannerUrl()), "/myShop"),
                check("description", notBlank(s.getDescriptionEn()), "/myShop"),
                check("contact", notBlank(s.getMobile()), "/myShop"),
                check("address", notBlank(s.getAddress()), "/myShop"),
                check("location", s.getLatitude() != null && s.getLongitude() != null, "/myShop"),
                check("hours", s.getOpeningTime() != null && s.getClosingTime() != null, "/myShop"),
                check("deliveryRadius", s.getDeliveryRadiusKm() != null, "/myShop"),
                check("prepTime", s.getAvgPreparationMinutes() != null, "/myShop"));
        return build("Shop", "shop", "shop", s.getUuid(), can, checks, "/myShop", false);
    }

    @Transactional(readOnly = true)
    public Response restaurant(SystemUser user) {
        var staff = restaurantAccess.currentStaff(user);
        RestaurantMaster s = staff.getRestaurant();
        Predicate<String> can = a -> restaurantAccess.has(staff, RestaurantActivity.valueOf(a));
        List<Map<String, Object>> checks = List.of(
                check("logo", notBlank(s.getLogoUrl()), "/myRestaurant"),
                check("banner", notBlank(s.getBannerUrl()), "/myRestaurant"),
                check("description", notBlank(s.getDescriptionEn()), "/myRestaurant"),
                check("contact", notBlank(s.getMobile()), "/myRestaurant"),
                check("address", notBlank(s.getAddress()), "/myRestaurant"),
                check("location", s.getLatitude() != null && s.getLongitude() != null, "/myRestaurant"),
                check("hours", s.getOpeningTime() != null && s.getClosingTime() != null, "/myRestaurant"),
                check("deliveryRadius", s.getDeliveryRadiusKm() != null, "/myRestaurant"),
                check("prepTime", s.getAvgPreparationMinutes() != null, "/myRestaurant"));
        return build("Restaurant", "restaurant", "restaurant", s.getUuid(), can, checks, "/myRestaurant", true);
    }

    /**
     * @param E     entity prefix ("Shop" / "Restaurant")
     * @param f     field of the item / category / staff entities that points to the store
     * @param route route prefix of the panel pages (shopItem, restaurantItem ...)
     */
    private Response build(String E, String f, String route, String storeUuid, Predicate<String> can,
                           List<Map<String, Object>> checks, String profileLink, boolean restaurant) {
        Response r = new Response();
        r.setScope(route);
        String items = E + "Item";
        String where = " where i.deleted = false and i." + f + ".uuid = ?1";
        boolean food = restaurant;

        Section attention = r.section("attention");
        Section orders = r.section("orders");
        Section catalogue = r.section("catalogue");
        Section team = r.section("team");
        Section profile = r.section("profile");

        if (can.test("ORDER_VIEW")) {
            String ow = " from StoreOrder o where o.deleted = false and o." + f + ".uuid = ?1";
            String ordersLink = "/" + route + "Order";
            attention.add(alert("newOrders", q.count("select count(o)" + ow + " and o.status = ?2",
                    storeUuid, com.bt.fretchain.primary.pojo.enums.StoreOrderStatus.PLACED), "danger", ordersLink));
            orders.add(stat("activeOrders", q.count("select count(o)" + ow + " and o.status in ?2", storeUuid,
                    com.bt.fretchain.primary.pojo.enums.StoreOrderStatus.ACTIVE), "NUMBER", "active", "orange", ordersLink));
            orders.add(stat("ordersToday", q.count("select count(o)" + ow + " and o.createdAt >= ?2", storeUuid, DashboardQuery.startOfToday()),
                    "NUMBER", "today", "indigo", ordersLink));
            orders.add(stat("salesToday", q.sum("select sum(o.totalAmount)" + ow + " and o.status = ?2 and o.createdAt >= ?3", storeUuid,
                    com.bt.fretchain.primary.pojo.enums.StoreOrderStatus.DELIVERED, DashboardQuery.startOfToday()), "MONEY", "money", "green", ordersLink));
            orders.add(stat("deliveredOrders", q.count("select count(o)" + ow + " and o.status = ?2", storeUuid,
                    com.bt.fretchain.primary.pojo.enums.StoreOrderStatus.DELIVERED), "NUMBER", "delivered", "green", ordersLink));
            orders.add(trend("ordersTrend", q.daily("select function('date', o.createdAt), count(o) from StoreOrder o where o.deleted = false and o." + f
                    + ".uuid = ?2 and o.createdAt >= ?1 group by function('date', o.createdAt)", 14, storeUuid), "NUMBER").size("lg"));
            List<Map<String, Object>> recentOrders = new ArrayList<>();
            for (Object[] o : q.rows("select o.orderNumber, o.contactName, o.status, o.totalAmount, o.uuid" + ow + " order by o.createdAt desc", 6, storeUuid)) {
                recentOrders.add(row(String.valueOf(o[0]), String.valueOf(o[1]), String.valueOf(o[2]),
                        AdminDashboardService.tone(String.valueOf(o[2])), o[3], ordersLink + "/" + o[4]));
            }
            orders.add(list("recentOrders", recentOrders, ordersLink));
        }

        if (can.test("ITEM_VIEW")) {
            long total = q.count("select count(i) from " + items + " i" + where, storeUuid);
            long unavailable = q.count("select count(i) from " + items + " i" + where + " and i.available = false", storeUuid);
            catalogue.add(stat(food ? "dishes" : "items", total, "NUMBER", "items", "blue", "/" + route + "Item"));
            catalogue.add(stat("availableItems", total - unavailable, "NUMBER", "available", "green", "/" + route + "Item"));
            catalogue.add(stat("unavailableItems", unavailable, "NUMBER", "unavailable", "red", "/" + route + "Item"));
            catalogue.add(stat("featuredItems", q.count("select count(i) from " + items + " i" + where + " and i.featured = true", storeUuid), "NUMBER", "featured", "orange", "/" + route + "Item"));
            catalogue.add(breakdown("itemsByCategory", q.breakdown("select c.nameEn, count(i) from " + items + " i join i.category c" + where + " group by c.nameEn order by count(i) desc", 8, storeUuid), "NUMBER"));
            if (food) {
                catalogue.add(breakdown("itemsByFoodType", q.breakdown("select i.foodType, count(i) from " + items + " i" + where + " group by i.foodType", 0, storeUuid), "NUMBER"));
            } else {
                long low = q.count("select count(i) from " + items + " i" + where + " and i.available = true and i.stockQuantity <= " + LOW_STOCK, storeUuid);
                attention.add(alert("lowStock", low, "warning", "/" + route + "Item"));
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Object[] o : q.rows("select i.nameEn, i.stockQuantity, c.nameEn from " + items + " i join i.category c" + where
                        + " and i.available = true and i.stockQuantity <= " + LOW_STOCK + " order by i.stockQuantity asc", 6, storeUuid)) {
                    rows.add(row(String.valueOf(o[0]), String.valueOf(o[2]), "LOW", "warning", o[1], null));
                }
                if (!rows.isEmpty()) {
                    catalogue.add(list("lowStockItems", rows, "/" + route + "Item"));
                }
            }
            attention.add(alert("itemsWithoutImage", q.count("select count(i) from " + items + " i" + where + " and (i.imageUrl is null or i.imageUrl = '')", storeUuid), "info", "/" + route + "Item"));
            attention.add(alert("unavailableItemsAlert", unavailable, "warning", "/" + route + "Item"));

            List<Map<String, Object>> recent = new ArrayList<>();
            for (Object[] o : q.rows("select i.nameEn, c.nameEn, i.price, i.available, i.uuid from " + items + " i join i.category c" + where + " order by i.createdAt desc", 6, storeUuid)) {
                recent.add(row(String.valueOf(o[0]), String.valueOf(o[1]), Boolean.TRUE.equals(o[3]) ? "AVAILABLE" : "UNAVAILABLE",
                        Boolean.TRUE.equals(o[3]) ? "success" : "danger", o[2], "/upsert" + E + "Item/" + o[4]));
            }
            catalogue.add(list("recentItems", recent, "/" + route + "Item"));
        }
        if (can.test("CATEGORY_VIEW")) {
            catalogue.add(stat("categories", q.count("select count(i) from " + E + "Category i" + where, storeUuid), "NUMBER", "category", "indigo", "/" + route + "Category"));
        }
        if (can.test("SUBCATEGORY_VIEW")) {
            catalogue.add(stat("subCategories", q.count("select count(i) from " + E + "SubCategory i" + where, storeUuid), "NUMBER", "subcategory", "purple", "/" + route + "SubCategory"));
        }
        if (can.test("STAFF_VIEW")) {
            team.add(stat("staffCount", q.count("select count(i) from " + E + "Staff i" + where + " and i.owner = false", storeUuid), "NUMBER", "staff", "teal", "/" + route + "Staff"));
            team.add(stat("activeStaff", q.count("select count(i) from " + E + "Staff i" + where + " and i.owner = false and i.active = true", storeUuid), "NUMBER", "online", "green", "/" + route + "Staff"));
        }
        if (can.test("ROLE_VIEW")) {
            team.add(stat("rolesCount", q.count("select count(i) from " + E + "Role i" + where, storeUuid), "NUMBER", "role", "orange", "/" + route + "Role"));
        }
        if (can.test("PROFILE_VIEW")) {
            // the catalogue also counts towards "ready for customers"
            List<Map<String, Object>> all = new ArrayList<>(checks);
            all.add(check("firstCategory", q.count("select count(i) from " + E + "Category i where i.deleted = false and i." + f + ".uuid = ?1", storeUuid) > 0, "/" + route + "Category"));
            all.add(check("firstItem", q.count("select count(i) from " + items + " i where i.deleted = false and i." + f + ".uuid = ?1", storeUuid) > 0, "/" + route + "Item"));
            profile.add(checklist("profileCompleteness", all).put("link", profileLink));
        }
        return r.prune();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
