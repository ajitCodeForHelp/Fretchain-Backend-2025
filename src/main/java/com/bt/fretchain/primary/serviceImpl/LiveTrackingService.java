package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.LiveDTO;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.StoreOrder;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Live map data. The platform admin sees every driver; a fleet owner sees only their own drivers
 * (vendor = the fleet owner's id). A vehicle has no tracker of its own: its position is its driver's.
 */
@Service
public class LiveTrackingService {

    /** After this long without a position an online driver is flagged "not reporting". */
    private static final Duration STALE_AFTER = Duration.ofMinutes(5);
    /** Drivers that were last seen long ago are not drawn as current positions of anything useful. */
    private static final List<OrderStatus> TRIP_STATUSES = List.of(
            OrderStatus.DRIVER_ASSIGNED, OrderStatus.ACCEPTED, OrderStatus.PICKUP_STARTED, OrderStatus.PICKUP_ARRIVED,
            OrderStatus.LOADING_STARTED, OrderStatus.LOADING_COMPLETED, OrderStatus.IN_TRANSIT, OrderStatus.DROP_ARRIVED,
            OrderStatus.UNLOADING_STARTED, OrderStatus.UNLOADING_COMPLETED);
    private static final List<StoreOrderStatus> STORE_TRIP = List.of(
            StoreOrderStatus.ACCEPTED, StoreOrderStatus.PREPARING, StoreOrderStatus.READY, StoreOrderStatus.OUT_FOR_DELIVERY);

    @PersistenceContext
    private EntityManager em;

    /**
     * @param vendorId  null for the platform admin, the fleet owner's id for a fleet owner
     * @param state     optional ON_ORDER / ONLINE / OFFLINE
     * @param search    optional text matched against name, mobile, vehicle number
     */
    @Transactional(readOnly = true)
    public LiveDTO.Response drivers(Long vendorId, String state, String search) {
        String jpql = "select d from Driver d left join fetch d.vehicle v where d.deleted = false and d.active = true"
                + (vendorId != null ? " and d.vendor.recordId = :vendor" : "") + " order by d.fullName";
        var q = em.createQuery(jpql, Driver.class);
        if (vendorId != null) q.setParameter("vendor", vendorId);
        List<Driver> drivers = q.getResultList();

        Map<Long, FreightOrder> freight = new HashMap<>();
        Map<Long, StoreOrder> store = new HashMap<>();
        if (!drivers.isEmpty()) {
            List<Long> ids = drivers.stream().map(Driver::getRecordId).collect(Collectors.toList());
            em.createQuery("select o from FreightOrder o where o.deleted = false and o.driver.recordId in :ids and o.orderStatus in :st", FreightOrder.class)
                    .setParameter("ids", ids).setParameter("st", TRIP_STATUSES).getResultList()
                    .forEach(o -> freight.put(o.getDriver().getRecordId(), o));
            em.createQuery("select o from StoreOrder o where o.deleted = false and o.driver.recordId in :ids and o.status in :st", StoreOrder.class)
                    .setParameter("ids", ids).setParameter("st", STORE_TRIP).getResultList()
                    .forEach(o -> store.put(o.getDriver().getRecordId(), o));
        }

        LiveDTO.Response out = new LiveDTO.Response();
        LocalDateTime now = LocalDateTime.now();
        String text = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        for (Driver d : drivers) {
            LiveDTO.Driver r = new LiveDTO.Driver();
            r.setUuid(d.getUuid());
            r.setName(d.getFullName());
            r.setMobile((d.getCountryCode() == null ? "" : d.getCountryCode()) + (d.getMobile() == null ? "" : d.getMobile()));
            r.setPhotoUrl(d.getProfileImageUrl());
            if (d.getVehicle() != null) {
                r.setVehicleNumber(d.getVehicle().getVehicleNumber());
                if (d.getVehicle().getVehicleCategory() != null) r.setVehicleType(d.getVehicle().getVehicleCategory().getNameEn());
            }
            if (d.getVendor() != null) r.setVendorName(d.getVendor().getFullName());
            r.setLat(d.getCurrentLat());
            r.setLng(d.getCurrentLng());
            r.setLastSeenAt(d.getLastLocationUpdatedAt());
            if (d.getLastLocationUpdatedAt() != null) {
                r.setSecondsAgo(Math.max(0, Duration.between(d.getLastLocationUpdatedAt(), now).getSeconds()));
            }

            FreightOrder fo = freight.get(d.getRecordId());
            StoreOrder so = store.get(d.getRecordId());
            boolean online = Boolean.TRUE.equals(d.getOnlineStatus());
            if (fo != null) {
                r.setState("ON_ORDER");
                r.setOrderType("FREIGHT");
                r.setOrderUuid(fo.getUuid());
                r.setOrderNumber(fo.getOrderNumber());
                r.setOrderStatus(pretty(fo.getOrderStatus().name()));
                r.setPickupAddress(fo.getPickupAddress() == null ? null : fo.getPickupAddress().getFullAddress());
            } else if (so != null) {
                r.setState("ON_ORDER");
                r.setOrderType(so.getStoreType() == StoreType.SHOP ? "SHOP" : "FOOD");
                r.setOrderUuid(so.getUuid());
                r.setOrderNumber(so.getOrderNumber());
                r.setOrderStatus(pretty(so.getStatus().name()));
                r.setPickupAddress(so.getStoreName());
                r.setDropAddress(so.getDeliveryAddress());
            } else {
                r.setState(online ? "ONLINE" : "OFFLINE");
            }
            r.setStale(!"OFFLINE".equals(r.getState())
                    && (d.getLastLocationUpdatedAt() == null || Duration.between(d.getLastLocationUpdatedAt(), now).compareTo(STALE_AFTER) > 0));

            if (!text.isEmpty() && !(contains(r.getName(), text) || contains(r.getMobile(), text) || contains(r.getVehicleNumber(), text))) continue;
            // counts describe the whole fleet that matches the search, whatever state is picked
            LiveDTO.Counts c = out.getCounts();
            c.setTotal(c.getTotal() + 1);
            switch (r.getState()) {
                case "ON_ORDER" -> c.setOnOrder(c.getOnOrder() + 1);
                case "ONLINE" -> c.setOnline(c.getOnline() + 1);
                default -> c.setOffline(c.getOffline() + 1);
            }
            if (r.getLat() == null || r.getLng() == null) c.setNoLocation(c.getNoLocation() + 1);
            if (r.isStale()) c.setStale(c.getStale() + 1);

            if (state != null && !state.isBlank() && !state.equalsIgnoreCase(r.getState())) continue;
            out.getItems().add(r);
        }
        return out;
    }

    private static boolean contains(String value, String lowerText) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerText);
    }

    private static String pretty(String code) {
        String t = code.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
