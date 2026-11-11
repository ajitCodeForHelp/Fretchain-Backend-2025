package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.StoreOrderDTO.*;
import com.bt.fretchain.primary.dto.DispatchDTO;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.StoreOrder;
import com.bt.fretchain.primary.pojo.enums.AssignmentMode;
import com.bt.fretchain.primary.pojo.enums.DeliveryService;
import com.bt.fretchain.primary.repository.DriverRepository;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import com.bt.fretchain.primary.repository.StoreOrderRepository;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The "Orders" section of a shop / restaurant login. The store always comes from the login
 * (see the partner controllers); permissions (ORDER_VIEW / ORDER_STATUS) are checked there.
 */
@Service
@RequiredArgsConstructor
public class StoreOrderPartnerService {

    private final StoreOrderRepository orderRepository;
    private final StoreOrderService orderService;
    private final DashboardQuery q;
    private final DispatchService dispatch;
    private final DriverRepository driverRepository;

    @Transactional(readOnly = true)
    public DataTableResponsePacket<Card> list(StoreType type, Long storeId, PartnerListRequest req, boolean canChangeStatus) {
        Specification<StoreOrder> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            p.add(cb.equal(root.get(field(type)).get("recordId"), storeId));
            if (req.getStatus() != null) {
                p.add(cb.equal(root.get("status"), req.getStatus()));
            } else if (req.getStatuses() != null && !req.getStatuses().isEmpty()) {
                p.add(root.get("status").in(req.getStatuses()));
            } else if (req.getActive() != null) {
                Predicate active = root.get("status").in(StoreOrderStatus.ACTIVE);
                p.add(req.getActive() ? active : cb.not(active));
            }
            String pattern = SearchUtils.containsPattern(req.getSearch());
            if (pattern != null) {
                char esc = SearchUtils.LIKE_ESCAPE;
                p.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern, esc),
                        cb.like(cb.lower(root.get("contactName")), pattern, esc),
                        cb.like(cb.lower(root.get("contactMobile")), pattern, esc)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        // oldest first for orders in progress (first come, first served), newest first otherwise
        boolean fifo = req.getStatus() == null && (Boolean.TRUE.equals(req.getActive())
                || (req.getStatuses() != null && req.getStatuses().stream().anyMatch(StoreOrderStatus::isActive)));
        Sort sort = Sort.by(fifo ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        return PaginationUtils.createResponse(orderRepository.findAll(spec,
                PageRequest.of(req.getPageNumber(), req.getPageSize(), sort))
                .map(o -> rowCard(o, canChangeStatus, false)));
    }

    /** List row with the buttons the viewer may use (store: its own steps; admin: also cancel and driver). */
    private Card rowCard(StoreOrder o, boolean canChange, boolean admin) {
        Card c = orderService.toCard(o);
        if (canChange) {
            c.setNextStatuses(new ArrayList<>(admin ? o.getStatus().nextForAdmin() : o.getStatus().nextForStore()));
            c.setCanAssignDriver(admin && canAssign(o));
        }
        return c;
    }

    private static boolean canAssign(StoreOrder o) {
        return o.getStatus() == StoreOrderStatus.ACCEPTED || o.getStatus() == StoreOrderStatus.PREPARING
                || o.getStatus() == StoreOrderStatus.READY;
    }

    // ================================================================ platform admin (all stores)

    @Transactional(readOnly = true)
    public DataTableResponsePacket<Card> adminList(AdminListRequest req, boolean canChangeStatus) {
        Specification<StoreOrder> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (req.getStoreType() != null) {
                p.add(cb.equal(root.get("storeType"), req.getStoreType()));
            }
            if (req.getStoreUuid() != null && !req.getStoreUuid().isBlank()) {
                p.add(cb.or(
                        cb.equal(root.join("shop", jakarta.persistence.criteria.JoinType.LEFT).get("uuid"), req.getStoreUuid()),
                        cb.equal(root.join("restaurant", jakarta.persistence.criteria.JoinType.LEFT).get("uuid"), req.getStoreUuid())));
            }
            if (req.getStatus() != null) {
                p.add(cb.equal(root.get("status"), req.getStatus()));
            } else if (req.getStatuses() != null && !req.getStatuses().isEmpty()) {
                p.add(root.get("status").in(req.getStatuses()));
            } else if (req.getActive() != null) {
                Predicate active = root.get("status").in(StoreOrderStatus.ACTIVE);
                p.add(req.getActive() ? active : cb.not(active));
            }
            String pattern = SearchUtils.containsPattern(req.getSearch());
            if (pattern != null) {
                char esc = SearchUtils.LIKE_ESCAPE;
                p.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern, esc),
                        cb.like(cb.lower(root.get("contactName")), pattern, esc),
                        cb.like(cb.lower(root.get("contactMobile")), pattern, esc),
                        cb.like(cb.lower(root.get("storeName")), pattern, esc)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        boolean fifo = req.getStatus() == null && (Boolean.TRUE.equals(req.getActive())
                || (req.getStatuses() != null && req.getStatuses().stream().anyMatch(StoreOrderStatus::isActive)));
        Sort sort = Sort.by(fifo ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        return PaginationUtils.createResponse(orderRepository.findAll(spec,
                PageRequest.of(req.getPageNumber(), req.getPageSize(), sort))
                .map(o -> rowCard(o, canChangeStatus, true)));
    }

    @Transactional(readOnly = true)
    public Detail adminDetail(String uuid, boolean canChangeStatus) {
        StoreOrder o = orderRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
        Detail d = orderService.toDetail(o);
        d.setCancellable(false);
        d.setNextStatuses(canChangeStatus ? new ArrayList<>(o.getStatus().nextForAdmin()) : new ArrayList<>());
        d.setCanAssignDriver(canChangeStatus && canAssign(o));
        return d;
    }

    // ---------------------------------------------------------------- driver assignment

    /** Ready drivers around the store, nearest first (manual assignment list). */
    @Transactional(readOnly = true)
    public List<DispatchDTO.Candidate> adminDriverCandidates(String uuid, Double radiusKm) {
        StoreOrder o = findAny(uuid);
        double[] at = storeLocation(o);
        return dispatch.candidates(serviceOf(o), at[0], at[1], radiusKm, 30);
    }

    @Transactional
    public Detail adminAssignDriver(String uuid, String driverUuid, SystemUser actor) {
        StoreOrder o = findAny(uuid);
        if (!canAssign(o)) {
            throw new BadRequestException("storeorder.driver.notnow");
        }
        if (driverUuid == null || driverUuid.isBlank()) {
            orderService.releaseDriver(o);
            orderService.log(o, o.getStatus(), "Driver removed", "Admin: " + actor.getFullName());
        } else {
            Driver d = driverRepository.findByUuidAndDeletedFalse(driverUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
            if (o.getDriver() != null && o.getDriver().getRecordId().equals(d.getRecordId())) {
                throw new BadRequestException("storeorder.driver.same");
            }
            if (dispatch.blockingReason(d, serviceOf(o)) != null) {
                throw new BadRequestException("storeorder.driver.notready");
            }
            assign(o, d, "Admin: " + actor.getFullName());
        }
        o.setModifiedBy(actor.getRecordId());
        orderRepository.save(o);
        return adminDetail(uuid, true);
    }

    /** The nearest ready driver, as AUTO mode would pick. */
    @Transactional
    public Detail adminAutoAssign(String uuid, SystemUser actor) {
        StoreOrder o = findAny(uuid);
        if (!canAssign(o)) {
            throw new BadRequestException("storeorder.driver.notnow");
        }
        if (!autoAssign(o, "Admin: " + actor.getFullName())) {
            throw new BadRequestException("storeorder.driver.none");
        }
        orderRepository.save(o);
        return adminDetail(uuid, true);
    }

    private void assign(StoreOrder o, Driver d, String by) {
        orderService.releaseDriver(o);          // a driver changed: the old one is free again
        d.setAvailable(false);
        o.setDriver(d);
        o.setDriverAssignedAt(java.time.LocalDateTime.now());
        orderService.log(o, o.getStatus(), "Driver assigned: " + d.getFullName(), by);
    }

    /** Picks the nearest ready driver around the store. Returns false when nobody is available. */
    private boolean autoAssign(StoreOrder o, String by) {
        double[] at = storeLocation(o);
        var pick = dispatch.pick(serviceOf(o), at[0], at[1]);
        if (pick.isEmpty()) {
            return false;
        }
        Driver d = driverRepository.findByUuidAndDeletedFalse(pick.get().getDriverUuid()).orElse(null);
        if (d == null) {
            return false;
        }
        assign(o, d, by);
        return true;
    }

    private static DeliveryService serviceOf(StoreOrder o) {
        return o.getStoreType() == StoreType.SHOP ? DeliveryService.SHOP_DELIVERY : DeliveryService.FOOD_DELIVERY;
    }

    private static double[] storeLocation(StoreOrder o) {
        var lat = o.getStoreType() == StoreType.SHOP ? o.getShop().getLatitude() : o.getRestaurant().getLatitude();
        var lng = o.getStoreType() == StoreType.SHOP ? o.getShop().getLongitude() : o.getRestaurant().getLongitude();
        return new double[]{lat.doubleValue(), lng.doubleValue()};
    }

    private StoreOrder findAny(String uuid) {
        return orderRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    /** Admin override: the same steps as the store, plus cancelling (reason required; stock and coupon go back). */
    @Transactional
    public Detail adminUpdateStatus(String uuid, StatusUpdate req, SystemUser actor) {
        StoreOrder o = orderRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
        StoreOrderStatus next = req.getStatus();
        if (!o.getStatus().nextForAdmin().contains(next)) {
            throw new BadRequestException("storeorder.status.invalid");
        }
        applyStatus(o, next, req.getReason(), actor.getRecordId(), "Admin: " + actor.getFullName());
        orderRepository.save(o);
        Detail d = orderService.toDetail(o);
        d.setCancellable(false);
        d.setNextStatuses(new ArrayList<>(next.nextForAdmin()));
        return d;
    }

    @Transactional(readOnly = true)
    public Summary adminSummary(StoreType type) {
        Summary s = new Summary();
        for (StoreOrderStatus st : StoreOrderStatus.values()) {
            s.getCounts().put(st.name(), 0L);
        }
        String where = " where o.deleted = false" + (type == null ? "" : " and o.storeType = ?1");
        Object[] p = type == null ? new Object[0] : new Object[]{type};
        for (Object[] r : q.rows("select o.status, count(o) from StoreOrder o" + where + " group by o.status", 0, p)) {
            s.getCounts().put(String.valueOf(r[0]), ((Number) r[1]).longValue());
        }
        s.setActiveOrders(StoreOrderStatus.ACTIVE.stream().mapToLong(st -> s.getCounts().get(st.name())).sum());
        Object[] p2 = type == null ? new Object[]{DashboardQuery.startOfToday()} : new Object[]{type, DashboardQuery.startOfToday()};
        String today = type == null ? " and o.createdAt >= ?1" : " and o.createdAt >= ?2";
        s.setOrdersToday(q.count("select count(o) from StoreOrder o" + where + today, p2));
        Object[] p3 = type == null ? new Object[]{StoreOrderStatus.DELIVERED, DashboardQuery.startOfToday()}
                : new Object[]{type, StoreOrderStatus.DELIVERED, DashboardQuery.startOfToday()};
        String sales = type == null ? " and o.status = ?1 and o.createdAt >= ?2" : " and o.status = ?2 and o.createdAt >= ?3";
        s.setSalesToday(q.sum("select sum(o.totalAmount) from StoreOrder o" + where + sales, p3));
        return s;
    }

    @Transactional(readOnly = true)
    public Detail detail(StoreType type, Long storeId, String uuid, boolean canChangeStatus) {
        StoreOrder o = find(type, storeId, uuid);
        Detail d = orderService.toDetail(o);
        d.setCancellable(false);
        d.setNextStatuses(canChangeStatus ? new ArrayList<>(o.getStatus().nextForStore()) : new ArrayList<>());
        return d;
    }

    @Transactional
    public Detail updateStatus(StoreType type, Long storeId, String uuid, StatusUpdate req, SystemUser actor) {
        StoreOrder o = find(type, storeId, uuid);
        StoreOrderStatus next = req.getStatus();
        if (!o.getStatus().nextForStore().contains(next)) {
            throw new BadRequestException("storeorder.status.invalid");
        }
        applyStatus(o, next, req.getReason(), actor.getRecordId(), actor.getFullName());
        orderRepository.save(o);
        Detail d = orderService.toDetail(o);
        d.setCancellable(false);
        d.setNextStatuses(new ArrayList<>(next.nextForStore()));
        return d;
    }

    /** Counts per status for the tabs, plus today's figures. */
    @Transactional(readOnly = true)
    public Summary summary(StoreType type, Long storeId) {
        String f = field(type);
        Summary s = new Summary();
        for (StoreOrderStatus st : StoreOrderStatus.values()) {
            s.getCounts().put(st.name(), 0L);
        }
        for (Object[] r : q.rows("select o.status, count(o) from StoreOrder o where o.deleted = false and o." + f
                + ".recordId = ?1 group by o.status", 0, storeId)) {
            s.getCounts().put(String.valueOf(r[0]), ((Number) r[1]).longValue());
        }
        s.setActiveOrders(StoreOrderStatus.ACTIVE.stream().mapToLong(st -> s.getCounts().get(st.name())).sum());
        s.setOrdersToday(q.count("select count(o) from StoreOrder o where o.deleted = false and o." + f
                + ".recordId = ?1 and o.createdAt >= ?2", storeId, DashboardQuery.startOfToday()));
        BigDecimal sales = q.sum("select sum(o.totalAmount) from StoreOrder o where o.deleted = false and o." + f
                + ".recordId = ?1 and o.status = ?2 and o.createdAt >= ?3", storeId, StoreOrderStatus.DELIVERED, DashboardQuery.startOfToday());
        s.setSalesToday(sales);
        return s;
    }

    /** Moves the order to `next`: reason for reject / cancel, stock and coupon released, cash marked paid on delivery. */
    private void applyStatus(StoreOrder o, StoreOrderStatus next, String reason, Long actorId, String actorName) {
        boolean stops = next == StoreOrderStatus.REJECTED || next == StoreOrderStatus.CANCELLED;
        if (stops && (reason == null || reason.isBlank())) {
            throw new BadRequestException("approval.reason.required");
        }
        o.setStatus(next);
        if (stops) {
            o.setStatusReason(reason.trim());
        }
        o.setModifiedBy(actorId);
        orderService.log(o, next, stops ? reason.trim() : null, actorName);
        if (stops) {
            orderService.releaseOrder(o);       // stock and coupon go back
        }
        if (next == StoreOrderStatus.DELIVERED) {
            o.setPaymentPaid(true);             // cash collected on delivery
            orderService.freeDriver(o);
        }
        // AUTO dispatch: when the store accepts, the nearest ready driver is assigned (admin can still change it)
        if (next == StoreOrderStatus.ACCEPTED && o.getDriver() == null
                && dispatch.settings().getMode() == AssignmentMode.AUTO) {
            autoAssign(o, "Auto dispatch");
        }
    }

    private StoreOrder find(StoreType type, Long storeId, String uuid) {
        return orderRepository.findByUuidAndDeletedFalse(uuid)
                .filter(o -> o.getStoreType() == type)
                .filter(o -> (type == StoreType.SHOP ? o.getShop().getRecordId() : o.getRestaurant().getRecordId()).equals(storeId))
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private static String field(StoreType type) {
        return type == StoreType.SHOP ? "shop" : "restaurant";
    }
}
