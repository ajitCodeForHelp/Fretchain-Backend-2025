package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.DashboardDTO;
import com.bt.fretchain.primary.dto.DashboardDTO.Section;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.PlatformActivity;
import com.bt.fretchain.primary.pojo.enums.RestaurantStatus;
import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.bt.fretchain.primary.dto.DashboardDTO.*;
import static com.bt.fretchain.primary.serviceImpl.DashboardQuery.*;

/**
 * Admin-panel dashboard. One dashboard for every admin login; each widget is guarded by the
 * menu permission of the data it shows, so full admins see everything, and an admin-staff
 * role sees only what its permissions cover (e.g. an Accountant: FINANCE_VIEW; an
 * Order Manager: ORDER_VIEW + DRIVER_VIEW).
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static String real(int n) {
        return "o.deleted = false and o.orderStatus not in ?" + n;
    }

    private final DashboardQuery q;
    private final PlatformAccessService access;

    @Transactional(readOnly = true)
    public Response build(SystemUser user, Integer daysParam) {
        int days = clampDays(daysParam);
        Set<PlatformActivity> can = access.activities(user);
        Response r = new Response();
        r.setScope("admin");
        r.setDays(days);

        // ---------------------------------------------------- needs attention
        Section attention = r.section("attention");
        if (can.contains(PlatformActivity.SHOP_VIEW)) {
            attention.add(alert("pendingShops", q.count("select count(s) from ShopMaster s where s.deleted = false and s.status = com.bt.fretchain.primary.pojo.enums.ShopStatus.PENDING"), "warning", "/shop"));
        }
        if (can.contains(PlatformActivity.RESTAURANT_VIEW)) {
            attention.add(alert("pendingRestaurants", q.count("select count(s) from RestaurantMaster s where s.deleted = false and s.status = com.bt.fretchain.primary.pojo.enums.RestaurantStatus.PENDING"), "warning", "/restaurant"));
        }
        if (can.contains(PlatformActivity.VEHICLE_VIEW)) {
            attention.add(alert("unverifiedVehicles", q.count("select count(v) from Vehicle v where v.deleted = false and v.active = true and (v.verified = false or v.verified is null)"), "warning", "/vehicle"));
        }
        if (can.contains(PlatformActivity.VENDOR_VIEW)) {
            attention.add(alert("pendingVendors", q.count("select count(v) from VendorMaster v where v.deleted = false and v.status = ?1", com.bt.fretchain.primary.pojo.enums.VendorStatus.PENDING), "warning", "/vendor"));
        }
        if (can.contains(PlatformActivity.DRIVER_VIEW)) {
            attention.add(alert("unverifiedDrivers", q.count("select count(d) from Driver d where d.deleted = false and d.active = true and (d.verified = false or d.verified is null)"), "warning", "/driver"));
            attention.add(alert("expiredLicenses", q.count("select count(d) from Driver d where d.deleted = false and d.active = true and d.licenseExpiryDate < current_date"), "danger", "/driver"));
        }
        if (can.contains(PlatformActivity.ORDER_VIEW)) {
            attention.add(alert("ordersWaitingDriver", q.count("select count(o) from FreightOrder o where o.deleted = false and o.orderStatus in ?1",
                    List.of(OrderStatus.PLACED, OrderStatus.DRIVER_REQUESTED)), "warning", "/orders"));
        }

        // ---------------------------------------------------- orders
        if (can.contains(PlatformActivity.ORDER_VIEW)) {
            Section s = r.section("orders");
            LocalDateTime from = LocalDateTime.now().minusDays(days);
            s.add(stat("totalOrders", q.count("select count(o) from FreightOrder o where " + real(1), DRAFTS), "NUMBER", "orders", "blue", "/orders"));
            s.add(stat("ordersToday", q.count("select count(o) from FreightOrder o where " + real(1) + " and o.createdAt >= ?2", DRAFTS, startOfToday()), "NUMBER", "today", "indigo", "/orders"));
            s.add(stat("activeOrders", q.count("select count(o) from FreightOrder o where o.deleted = false and o.orderStatus in ?1", ACTIVE), "NUMBER", "active", "orange", "/orders"));
            s.add(stat("deliveredOrders", q.count("select count(o) from FreightOrder o where o.deleted = false and o.orderStatus = ?1", OrderStatus.DELIVERED), "NUMBER", "delivered", "green", "/orders"));
            s.add(stat("cancelledOrders", q.count("select count(o) from FreightOrder o where o.deleted = false and o.orderStatus in ?1", CANCELLED), "NUMBER", "cancelled", "red", "/orders"));
            s.add(stat("customers", q.count("select count(u) from SystemUser u where u.deleted = false and u.userType = ?1", UserTypeEnum.CUSTOMER), "NUMBER", "customers", "teal", null));
            s.add(trend("ordersTrend", q.daily("select function('date', o.createdAt), count(o) from FreightOrder o where " + real(2) + " and o.createdAt >= ?1 group by function('date', o.createdAt)", days, DRAFTS), "NUMBER").size("lg"));
            s.add(breakdown("ordersByType", q.breakdown("select o.orderType, count(o) from FreightOrder o where " + real(1) + " and o.createdAt >= ?2 group by o.orderType order by count(o) desc", 0, DRAFTS, from), "NUMBER"));
            s.add(breakdown("ordersByStatus", q.breakdown("select o.orderStatus, count(o) from FreightOrder o where " + real(1) + " and o.createdAt >= ?2 group by o.orderStatus order by count(o) desc", 0, DRAFTS, from), "NUMBER"));
            s.add(list("recentOrders", recentOrders(), "/orders"));
        }

        // ---------------------------------------------------- finance
        if (can.contains(PlatformActivity.FINANCE_VIEW)) {
            Section s = r.section("finance");
            String paid = "o.deleted = false and o.orderStatus = ?1";
            s.add(stat("revenueTotal", q.sum("select sum(o.totalAmount) from FreightOrder o where " + paid, OrderStatus.DELIVERED), "MONEY", "money", "green", null));
            s.add(stat("revenueMonth", q.sum("select sum(o.totalAmount) from FreightOrder o where " + paid + " and o.createdAt >= ?2", OrderStatus.DELIVERED, startOfMonth()), "MONEY", "money", "blue", null));
            long delivered = q.count("select count(o) from FreightOrder o where " + paid, OrderStatus.DELIVERED);
            BigDecimal total = q.sum("select sum(o.totalAmount) from FreightOrder o where " + paid, OrderStatus.DELIVERED);
            s.add(stat("avgOrderValue", delivered == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(delivered), 0, java.math.RoundingMode.HALF_UP), "MONEY", "avg", "indigo", null));
            s.add(stat("couponDiscounts", q.sum("select sum(o.couponDiscountAmount) from FreightOrder o where " + paid, OrderStatus.DELIVERED), "MONEY", "coupon", "orange", null));
            s.add(trend("revenueTrend", q.daily("select function('date', o.createdAt), sum(o.totalAmount) from FreightOrder o where o.deleted = false and o.orderStatus = ?2 and o.createdAt >= ?1 group by function('date', o.createdAt)", days, OrderStatus.DELIVERED), "MONEY").size("lg"));
            s.add(breakdown("revenueByType", q.breakdown("select o.orderType, sum(o.totalAmount) from FreightOrder o where " + paid + " and o.createdAt >= ?2 group by o.orderType order by sum(o.totalAmount) desc", 0, OrderStatus.DELIVERED, LocalDateTime.now().minusDays(days)), "MONEY"));
        }

        // ---------------------------------------------------- fleet
        Section fleet = r.section("fleet");
        if (can.contains(PlatformActivity.VEHICLE_VIEW)) {
            fleet.add(stat("vehicles", q.count("select count(v) from Vehicle v where v.deleted = false"), "NUMBER", "vehicle", "blue", "/vehicle"));
            fleet.add(stat("verifiedVehicles", q.count("select count(v) from Vehicle v where v.deleted = false and v.verified = true"), "NUMBER", "verified", "green", "/vehicle"));
            fleet.add(breakdown("vehiclesByCategory", q.breakdown("select c.nameEn, count(v) from Vehicle v join v.vehicleCategory c where v.deleted = false group by c.nameEn order by count(v) desc", 8), "NUMBER"));
        }
        if (can.contains(PlatformActivity.DRIVER_VIEW)) {
            fleet.add(stat("drivers", q.count("select count(d) from Driver d where d.deleted = false"), "NUMBER", "driver", "orange", "/driver"));
            fleet.add(stat("driversOnline", q.count("select count(d) from Driver d where d.deleted = false and d.active = true and d.onlineStatus = true"), "NUMBER", "online", "green", "/driver"));
            fleet.add(stat("driversAvailable", q.count("select count(d) from Driver d where d.deleted = false and d.active = true and d.available = true"), "NUMBER", "available", "teal", "/driver"));
            fleet.add(stat("driversNoVehicle", q.count("select count(d) from Driver d where d.deleted = false and d.active = true and d.vehicle is null"), "NUMBER", "novehicle", "red", "/driver"));
        }
        if (can.contains(PlatformActivity.VENDOR_VIEW)) {
            fleet.add(stat("vendors", q.count("select count(v) from VendorMaster v where v.deleted = false"), "NUMBER", "vendor", "purple", "/vendor"));
            fleet.add(stat("activeVendors", q.count("select count(v) from VendorMaster v where v.deleted = false and v.active = true"), "NUMBER", "active", "green", "/vendor"));
            List<Map<String, Object>> pendingVendors = new ArrayList<>();
            for (Object[] o : q.rows("select v.companyName, v.city, v.createdAt, v.uuid from VendorMaster v where v.deleted = false and v.status = ?1 order by v.createdAt asc", 5, com.bt.fretchain.primary.pojo.enums.VendorStatus.PENDING)) {
                pendingVendors.add(row(String.valueOf(o[0]), o[1] == null ? "" : String.valueOf(o[1]), "PENDING", "warning", String.valueOf(o[2]), "/vendorApproval/" + o[3]));
            }
            if (!pendingVendors.isEmpty()) {
                fleet.add(list("pendingVendorList", pendingVendors, "/vendor"));
            }
            fleet.add(breakdown("topVendors", q.breakdown("select v.vendor.fullName, count(v) from Vehicle v where v.deleted = false and v.vendor is not null group by v.vendor.fullName order by count(v) desc", 6), "NUMBER"));
        }

        // ---------------------------------------------------- shops / restaurants
        if (can.contains(PlatformActivity.SHOP_VIEW)) {
            Section s = r.section("shops");
            s.add(stat("shops", q.count("select count(x) from ShopMaster x where x.deleted = false"), "NUMBER", "shop", "blue", "/shop"));
            s.add(stat("shopItems", q.count("select count(i) from ShopItem i where i.deleted = false"), "NUMBER", "items", "indigo", "/shopItem"));
            s.add(breakdown("shopsByStatus", q.breakdown("select x.status, count(x) from ShopMaster x where x.deleted = false group by x.status", 0), "NUMBER"));
            s.add(list("pendingShopList", pending("ShopMaster", ShopStatus.PENDING, "/shopApproval/"), "/shop"));
        }
        if (can.contains(PlatformActivity.RESTAURANT_VIEW)) {
            Section s = r.section("restaurants");
            s.add(stat("restaurants", q.count("select count(x) from RestaurantMaster x where x.deleted = false"), "NUMBER", "restaurant", "orange", "/restaurant"));
            s.add(stat("dishes", q.count("select count(i) from RestaurantItem i where i.deleted = false"), "NUMBER", "items", "indigo", "/restaurantItem"));
            s.add(breakdown("restaurantsByStatus", q.breakdown("select x.status, count(x) from RestaurantMaster x where x.deleted = false group by x.status", 0), "NUMBER"));
            s.add(list("pendingRestaurantList", pending("RestaurantMaster", RestaurantStatus.PENDING, "/restaurantApproval/"), "/restaurant"));
        }

        // ---------------------------------------------------- marketing / team
        Section extras = r.section("platform");
        if (can.contains(PlatformActivity.COUPON_VIEW)) {
            extras.add(stat("activeCoupons", q.count("select count(c) from Coupon c where c.deleted = false and c.active = true and (c.endDate is null or c.endDate > ?1)", LocalDateTime.now()), "NUMBER", "coupon", "orange", "/coupon"));
        }
        if (can.contains(PlatformActivity.BANNER_VIEW)) {
            extras.add(stat("activeBanners", q.count("select count(b) from Banner b where b.deleted = false and b.active = true and (b.endDate is null or b.endDate > ?1)", LocalDateTime.now()), "NUMBER", "banner", "purple", "/banner"));
        }
        if (can.contains(PlatformActivity.ADMIN_STAFF_VIEW)) {
            extras.add(stat("adminStaff", q.count("select count(u) from SystemUser u where u.deleted = false and u.userType = ?1", UserTypeEnum.ADMIN_STAFF), "NUMBER", "staff", "teal", "/adminStaff"));
        }
        return r.prune();
    }

    private List<Map<String, Object>> recentOrders() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] o : q.rows("select o.orderNumber, o.orderType, o.orderStatus, o.totalAmount, o.createdAt, c.fullName from FreightOrder o left join o.customer c where o.deleted = false and o.orderStatus not in ?1 order by o.createdAt desc", 6, DRAFTS)) {
            rows.add(row(String.valueOf(o[0]), (o[5] == null ? "" : o[5] + " • ") + o[1], String.valueOf(o[2]), tone(String.valueOf(o[2])), o[3], null));
        }
        return rows;
    }

    private List<Map<String, Object>> pending(String entity, Object pendingStatus, String linkPrefix) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] o : q.rows("select x.nameEn, x.city, x.createdAt, x.uuid from " + entity + " x where x.deleted = false and x.status = ?1 order by x.createdAt asc", 5, pendingStatus)) {
            rows.add(row(String.valueOf(o[0]), o[1] == null ? "" : String.valueOf(o[1]), "PENDING", "warning", String.valueOf(o[2]), linkPrefix + o[3]));
        }
        return rows;
    }

    static String tone(String status) {
        if (status.startsWith("CANCELLED")) return "danger";
        if (status.equals("DELIVERED")) return "success";
        if (status.equals("PLACED") || status.equals("DRIVER_REQUESTED")) return "warning";
        return "info";
    }
}
