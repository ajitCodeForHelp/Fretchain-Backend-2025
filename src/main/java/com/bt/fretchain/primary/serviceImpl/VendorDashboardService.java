package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.DashboardDTO.Response;
import com.bt.fretchain.primary.dto.DashboardDTO.Section;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.VendorMaster;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static com.bt.fretchain.primary.dto.DashboardDTO.*;
import static com.bt.fretchain.primary.serviceImpl.DashboardQuery.*;

/** Dashboard of a vendor (fleet owner): own fleet, drivers, compliance alerts and orders. */
@Service
@RequiredArgsConstructor
public class VendorDashboardService {

    private final DashboardQuery q;
    private final VendorService vendorService;

    @Transactional(readOnly = true)
    public Response build(SystemUser user, Integer daysParam) {
        int days = clampDays(daysParam);
        VendorMaster v = vendorService.current(user);
        String me = user.getUuid();
        Response r = new Response();
        r.setScope("vendor");
        r.setDays(days);

        String veh = " where v.deleted = false and v.vendor.uuid = ?1";
        String drv = " where d.deleted = false and d.vendor.uuid = ?1";
        String ord = " where o.deleted = false and o.vendor.uuid = ?1";
        LocalDate soon = LocalDate.now().plusDays(30);

        // ---------------------------------------------------- needs attention
        Section attention = r.section("attention");
        attention.add(alert("unverifiedVehicles", q.count("select count(v) from Vehicle v" + veh + " and (v.verified = false or v.verified is null)", me), "warning", "/myVehicles"));
        attention.add(alert("vehiclesNoDriver", q.count("select count(v) from Vehicle v" + veh + " and v.active = true and v.driver is null", me), "info", "/myVehicles"));
        attention.add(alert("driversNoVehicle", q.count("select count(d) from Driver d" + drv + " and d.active = true and d.vehicle is null", me), "info", "/myDrivers"));
        attention.add(alert("expiredLicenses", q.count("select count(d) from Driver d" + drv + " and d.active = true and d.licenseExpiryDate < ?2", me, LocalDate.now()), "danger", "/myDrivers"));
        attention.add(alert("licensesExpiringSoon", q.count("select count(d) from Driver d" + drv + " and d.active = true and d.licenseExpiryDate >= ?2 and d.licenseExpiryDate <= ?3", me, LocalDate.now(), soon), "warning", "/myDrivers"));

        // ---------------------------------------------------- fleet
        Section fleet = r.section("fleet");
        long vehicles = q.count("select count(v) from Vehicle v" + veh, me);
        long verified = q.count("select count(v) from Vehicle v" + veh + " and v.verified = true", me);
        fleet.add(stat("vehicles", vehicles, "NUMBER", "vehicle", "blue", "/myVehicles"));
        fleet.add(stat("verifiedVehicles", verified, "NUMBER", "verified", "green", "/myVehicles"));
        fleet.add(stat("pendingVehicles", vehicles - verified, "NUMBER", "pending", "orange", "/myVehicles"));
        fleet.add(stat("drivers", q.count("select count(d) from Driver d" + drv, me), "NUMBER", "driver", "indigo", "/myDrivers"));
        fleet.add(stat("driversOnline", q.count("select count(d) from Driver d" + drv + " and d.active = true and d.onlineStatus = true", me), "NUMBER", "online", "green", "/myDrivers"));
        fleet.add(stat("driversAvailable", q.count("select count(d) from Driver d" + drv + " and d.active = true and d.available = true", me), "NUMBER", "available", "teal", "/myDrivers"));
        fleet.add(breakdown("vehiclesByCategory", q.breakdown("select c.nameEn, count(v) from Vehicle v join v.vehicleCategory c" + veh + " group by c.nameEn order by count(v) desc", 8, me), "NUMBER"));
        List<Map<String, Object>> licences = new ArrayList<>();
        for (Object[] o : q.rows("select d.fullName, d.licenseExpiryDate from Driver d" + drv + " and d.active = true and d.licenseExpiryDate <= ?2 order by d.licenseExpiryDate asc", 6, me, soon)) {
            boolean expired = ((LocalDate) o[1]).isBefore(LocalDate.now());
            licences.add(row(String.valueOf(o[0]), String.valueOf(o[1]), expired ? "EXPIRED" : "EXPIRING", expired ? "danger" : "warning", null, null));
        }
        if (!licences.isEmpty()) {
            fleet.add(list("licenseWatch", licences, "/myDrivers"));
        }

        // ---------------------------------------------------- orders carried
        Section orders = r.section("orders");
        long delivered = q.count("select count(o) from FreightOrder o" + ord + " and o.orderStatus = ?2", me, OrderStatus.DELIVERED);
        long active = q.count("select count(o) from FreightOrder o" + ord + " and o.orderStatus in ?2", me, ACTIVE);
        long total = q.count("select count(o) from FreightOrder o" + ord + " and o.orderStatus not in ?2", me, DRAFTS);
        if (total > 0) {
            orders.add(stat("totalOrders", total, "NUMBER", "orders", "blue", null));
            orders.add(stat("activeOrders", active, "NUMBER", "active", "orange", null));
            orders.add(stat("deliveredOrders", delivered, "NUMBER", "delivered", "green", null));
            orders.add(trend("ordersTrend", q.daily("select function('date', o.createdAt), count(o) from FreightOrder o where o.deleted = false and o.vendor.uuid = ?2 and o.orderStatus not in ?3 and o.createdAt >= ?1 group by function('date', o.createdAt)", days, me, DRAFTS), "NUMBER").size("lg"));
            List<Map<String, Object>> recent = new ArrayList<>();
            for (Object[] o : q.rows("select o.orderNumber, o.orderType, o.orderStatus, o.createdAt from FreightOrder o" + ord + " and o.orderStatus not in ?2 order by o.createdAt desc", 6, me, DRAFTS)) {
                recent.add(row(String.valueOf(o[0]), String.valueOf(o[1]), String.valueOf(o[2]), AdminDashboardService.tone(String.valueOf(o[2])), String.valueOf(o[3]), null));
            }
            orders.add(list("recentOrders", recent, null));
        }

        // ---------------------------------------------------- company profile
        Section profile = r.section("profile");
        profile.add(checklist("profileCompleteness", List.of(
                check("logo", notBlank(v.getLogoUrl()), "/myCompany"),
                check("contact", notBlank(v.getMobile()) && notBlank(v.getContactPerson()), "/myCompany"),
                check("address", notBlank(v.getAddress()) && notBlank(v.getCity()), "/myCompany"),
                check("legal", notBlank(v.getTaxNumber()) && notBlank(v.getRegistrationNumber()), "/myCompany"),
                check("firstVehicle", vehicles > 0, "/myVehicles"),
                check("firstDriver", q.count("select count(d) from Driver d" + drv, me) > 0, "/myDrivers"))).put("link", "/myCompany"));
        return r.prune();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
