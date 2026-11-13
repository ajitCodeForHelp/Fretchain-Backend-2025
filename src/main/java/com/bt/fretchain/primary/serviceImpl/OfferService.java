package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.DispatchDTO;
import com.bt.fretchain.primary.dto.OfferDTO;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.*;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.primary.service.DriverNotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Offers an order to drivers and handles their answers, for freight AND shop / food orders.
 *
 *   shop / food : one driver at a time, nearest first, store-offer seconds each (default 30)
 *   freight     : the nearest N drivers at once (default 3), first to accept wins, freight-offer seconds
 *                 (default 120) per round, then the next N
 *   manual      : admin / fleet owner picks a driver; that driver gets the same offer (accept / reject / time limit)
 *
 * A rejected or expired offer in AUTO mode moves on to the next driver(s) by itself. When nobody is left the order
 * simply stays without a driver and staff can assign one manually.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

    private final OrderDriverAssignmentRepository offers;
    private final DriverRepository drivers;
    private final FreightOrderStatusLogRepository freightLogs;
    private final DispatchService dispatch;
    private final DriverNotificationService push;

    @PersistenceContext
    private EntityManager em;

    // ================================================================ sending offers

    /** Staff chose this driver. Replaces any offer still open for the order. */
    @Transactional
    public OrderDriverAssignment offerManual(FreightOrder order, Driver driver, String by, Long actorId) {
        cancelOpen(order, "Replaced by a manual assignment");
        OrderDriverAssignment a = create(order, null, driver, AssignmentMode.MANUAL, 1, null, actorId);
        freightLog(order, "Offered to " + driver.getFullName() + " (" + by + ")", null);
        send(a);
        return a;
    }

    @Transactional
    public OrderDriverAssignment offerManual(StoreOrder order, Driver driver, String by, Long actorId) {
        cancelOpen(order, "Replaced by a manual assignment");
        OrderDriverAssignment a = create(null, order, driver, AssignmentMode.MANUAL, 1, null, actorId);
        storeLog(order, "Offered to " + driver.getFullName(), by);
        send(a);
        return a;
    }

    /** AUTO: the next driver (shop / food) or the next group of drivers (freight). Returns how many offers went out. */
    @Transactional
    public int dispatchNext(FreightOrder order) {
        return dispatchNext(order, false);
    }

    /** fresh = staff pressed "auto assign": drivers who declined or did not answer earlier are asked again. */
    @Transactional
    public int dispatchNext(FreightOrder order, boolean fresh) {
        if (order.getDriver() != null || !openFor(order).isEmpty() || !freightOpen(order)) {
            return 0;
        }
        DispatchDTO.Settings st = dispatch.settings();
        Set<Long> tried = fresh ? Set.of() : triedDrivers(offers.findByFreightOrderRecordIdAndDeletedFalse(order.getRecordId()));
        List<DispatchDTO.Candidate> next = dispatch.freightCandidates(order, st.getFreightRadiusKm(), st.getFreightWaveSize(), null, tried);
        return sendWave(order, null, next, nextWave(offers.findByFreightOrderRecordIdAndDeletedFalse(order.getRecordId())));
    }

    @Transactional
    public int dispatchNext(StoreOrder order) {
        return dispatchNext(order, false);
    }

    @Transactional
    public int dispatchNext(StoreOrder order, boolean fresh) {
        if (order.getDriver() != null || !openFor(order).isEmpty() || !storeOpen(order)) {
            return 0;
        }
        List<OrderDriverAssignment> history = offers.findByStoreOrderRecordIdAndDeletedFalse(order.getRecordId());
        double[] at = storePoint(order);
        DeliveryService service = order.getStoreType() == StoreType.SHOP ? DeliveryService.SHOP_DELIVERY : DeliveryService.FOOD_DELIVERY;
        Set<Long> tried = fresh ? Set.of() : triedDrivers(history);
        List<DispatchDTO.Candidate> ranked = dispatch.candidates(service, at[0], at[1], null, 50).stream()
                .filter(c -> !tried.contains(driverId(c.getDriverUuid())))
                .limit(1).collect(Collectors.toList());
        int sent = sendWave(null, order, ranked, nextWave(history));
        if (sent == 0 && history.stream().noneMatch(h -> h.getAssignmentStatus() == DriverAssignmentStatus.PENDING)) {
            log.info("No driver available for order {}", order.getOrderNumber());
        }
        return sent;
    }

    private int sendWave(FreightOrder fo, StoreOrder so, List<DispatchDTO.Candidate> list, int wave) {
        int n = 0;
        for (DispatchDTO.Candidate c : list) {
            Driver d = drivers.findByUuidAndDeletedFalse(c.getDriverUuid()).orElse(null);
            if (d == null) continue;
            OrderDriverAssignment a = create(fo, so, d, AssignmentMode.AUTO, wave, c.getDistanceKm(), null);
            send(a);
            n++;
        }
        if (n > 0) {
            String note = (so != null ? "Offered to the nearest driver" : "Offered to the " + n + " nearest driver" + (n > 1 ? "s" : ""))
                    + " (round " + wave + ")";
            if (so != null) storeLog(so, note, "Auto dispatch"); else freightLog(fo, note, null);
        }
        return n;
    }

    // ================================================================ driver answers

    @Transactional(noRollbackFor = BadRequestException.class)
    public OfferDTO.View accept(Driver driver, String uuid) {
        OrderDriverAssignment a = own(driver, uuid);
        if (a.getAssignmentStatus() != DriverAssignmentStatus.PENDING) {
            throw new BadRequestException("offer.no.longer.available");
        }
        if (a.getExpiresAt() != null && a.getExpiresAt().isBefore(LocalDateTime.now())) {
            close(a, DriverAssignmentStatus.EXPIRED, null);
            throw new BadRequestException("offer.expired");
        }
        if (!Boolean.TRUE.equals(driver.getAvailable())) {
            throw new BadRequestException("offer.driver.busy");
        }
        boolean freight = a.getFreightOrder() != null;
        Object lockedOrder = freight
                ? em.find(FreightOrder.class, a.getFreightOrder().getRecordId(), LockModeType.PESSIMISTIC_WRITE)
                : em.find(StoreOrder.class, a.getStoreOrder().getRecordId(), LockModeType.PESSIMISTIC_WRITE);
        em.refresh(a);
        if (a.getAssignmentStatus() != DriverAssignmentStatus.PENDING) {   // someone else won while we waited for the lock
            throw new BadRequestException("offer.no.longer.available");
        }
        if (freight) {
            FreightOrder o = (FreightOrder) lockedOrder;
            if (o.getDriver() != null || !freightOpen(o)) {
                close(a, DriverAssignmentStatus.CANCELLED, null);
                throw new BadRequestException("offer.taken");
            }
            o.setDriver(driver);
            o.setOrderStatus(OrderStatus.ACCEPTED);
            freightLog(o, "Driver accepted: " + driver.getFullName(), OrderStatus.ACCEPTED);
        } else {
            StoreOrder o = (StoreOrder) lockedOrder;
            if (o.getDriver() != null || !storeOpen(o)) {
                close(a, DriverAssignmentStatus.CANCELLED, null);
                throw new BadRequestException("offer.taken");
            }
            o.setDriver(driver);
            o.setDriverAssignedAt(LocalDateTime.now());
            storeLog(o, "Driver accepted: " + driver.getFullName(), driver.getFullName());
        }
        driver.setAvailable(false);
        a.setAssignmentStatus(DriverAssignmentStatus.ACCEPTED);
        a.setRespondedAt(LocalDateTime.now());
        offers.save(a);
        drivers.save(driver);
        // everyone else who was asked: too late
        List<OrderDriverAssignment> rest = freight
                ? offers.findByFreightOrderRecordIdAndAssignmentStatusAndDeletedFalse(a.getFreightOrder().getRecordId(), DriverAssignmentStatus.PENDING)
                : offers.findByStoreOrderRecordIdAndAssignmentStatusAndDeletedFalse(a.getStoreOrder().getRecordId(), DriverAssignmentStatus.PENDING);
        rest.stream().filter(x -> !x.getRecordId().equals(a.getRecordId())).forEach(x -> close(x, DriverAssignmentStatus.CANCELLED, "Taken by another driver"));
        return view(a);
    }

    @Transactional
    public void reject(Driver driver, String uuid, String reason) {
        OrderDriverAssignment a = own(driver, uuid);
        if (a.getAssignmentStatus() != DriverAssignmentStatus.PENDING) {
            throw new BadRequestException("offer.no.longer.available");
        }
        a.setAssignmentStatus(DriverAssignmentStatus.REJECTED);
        a.setRespondedAt(LocalDateTime.now());
        a.setRejectionReason(reason == null || reason.isBlank() ? null : reason.trim());
        offers.save(a);
        String note = "Driver " + driver.getFullName() + " declined" + (a.getRejectionReason() == null ? "" : ": " + a.getRejectionReason());
        afterOfferEnded(a, note);
    }

    // ================================================================ expiry (every 5 seconds)

    @Scheduled(fixedDelay = 5000, initialDelay = 15000)
    @Transactional
    public void expireDue() {
        List<OrderDriverAssignment> due = offers.findByAssignmentStatusAndExpiresAtBeforeAndDeletedFalse(DriverAssignmentStatus.PENDING, LocalDateTime.now());
        for (OrderDriverAssignment a : due) {
            try {
                close(a, DriverAssignmentStatus.EXPIRED, "No answer in time");
                afterOfferEnded(a, "No answer from " + a.getDriver().getFullName() + " in time");
            } catch (Exception e) {
                log.warn("Could not expire offer {}: {}", a.getUuid(), e.getMessage());
            }
        }
    }

    /** An offer ended without an accept: log it, and in AUTO mode carry on with the next driver(s). */
    private void afterOfferEnded(OrderDriverAssignment a, String note) {
        if (a.getFreightOrder() != null) {
            FreightOrder o = a.getFreightOrder();
            freightLog(o, note, null);
            if (AssignmentMode.AUTO.name().equals(a.getOfferMode())) dispatchNext(o);
        } else if (a.getStoreOrder() != null) {
            StoreOrder o = a.getStoreOrder();
            storeLog(o, note, "System");
            if (AssignmentMode.AUTO.name().equals(a.getOfferMode())) dispatchNext(o);
        }
    }

    // ================================================================ order ended / driver changed

    /** Open offers are withdrawn (order cancelled / rejected, or staff took over). */
    @Transactional
    public void cancelOpen(FreightOrder order, String reason) {
        openFor(order).forEach(x -> close(x, DriverAssignmentStatus.CANCELLED, reason));
    }

    @Transactional
    public void cancelOpen(StoreOrder order, String reason) {
        openFor(order).forEach(x -> close(x, DriverAssignmentStatus.CANCELLED, reason));
    }

    /** The offer a store order is currently waiting on (for the order list), or null. */
    @Transactional(readOnly = true)
    public OrderDriverAssignment currentOffer(StoreOrder order) {
        return openFor(order).stream().findFirst().orElse(null);
    }

    // ================================================================ driver's own view

    @Transactional(readOnly = true)
    public List<OfferDTO.View> pendingFor(Driver driver) {
        return offers.findByDriverRecordIdAndAssignmentStatusAndExpiresAtAfterAndDeletedFalseOrderByAssignedAtDesc(
                        driver.getRecordId(), DriverAssignmentStatus.PENDING, LocalDateTime.now())
                .stream().map(this::view).collect(Collectors.toList());
    }

    // ================================================================ helpers

    private OrderDriverAssignment own(Driver driver, String uuid) {
        OrderDriverAssignment a = offers.findByUuidAndDeletedFalse(uuid).orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
        if (!a.getDriver().getRecordId().equals(driver.getRecordId())) {
            throw new ResourceNotFoundException("record.notfound");
        }
        return a;
    }

    private OrderDriverAssignment create(FreightOrder fo, StoreOrder so, Driver d, AssignmentMode mode, int wave, Double km, Long actor) {
        DispatchDTO.Settings st = dispatch.settings();
        int seconds = fo != null ? st.getFreightOfferSeconds() : st.getStoreOfferSeconds();
        OrderDriverAssignment a = new OrderDriverAssignment();
        a.setFreightOrder(fo);
        a.setStoreOrder(so);
        a.setDriver(d);
        a.setAssignmentStatus(DriverAssignmentStatus.PENDING);
        a.setAssignedAt(LocalDateTime.now());
        a.setExpiresAt(LocalDateTime.now().plusSeconds(seconds));
        a.setOfferMode(mode.name());
        a.setWave(wave);
        a.setDistanceKm(km);
        a.setNotificationSent(false);
        a.setCreatedBy(actor);
        a.setModifiedBy(actor);
        return offers.save(a);
    }

    private void send(OrderDriverAssignment a) {
        boolean freight = a.getFreightOrder() != null;
        String type = freight ? "FREIGHT" : a.getStoreOrder().getStoreType() == StoreType.SHOP ? "SHOP" : "FOOD";
        String number = freight ? a.getFreightOrder().getOrderNumber() : a.getStoreOrder().getOrderNumber();
        String body = freight
                ? "Freight job" + (a.getDistanceKm() == null ? "" : " • " + a.getDistanceKm() + " km away") + ". Tap to accept."
                : (a.getStoreOrder().getStoreType() == StoreType.SHOP ? "Shop" : "Food") + " delivery from " + a.getStoreOrder().getStoreName()
                  + (a.getDistanceKm() == null ? "" : " • " + a.getDistanceKm() + " km away");
        push.sendOffer(a, type, number, body);
        offers.save(a);
    }

    private void close(OrderDriverAssignment a, DriverAssignmentStatus status, String reason) {
        if (a.getAssignmentStatus() != DriverAssignmentStatus.PENDING) return;
        a.setAssignmentStatus(status);
        a.setRespondedAt(LocalDateTime.now());
        offers.save(a);
        push.sendOfferClosed(a.getDriver(), a.getUuid(), reason == null ? status.name() : reason);
    }

    private List<OrderDriverAssignment> openFor(FreightOrder o) {
        return offers.findByFreightOrderRecordIdAndAssignmentStatusAndDeletedFalse(o.getRecordId(), DriverAssignmentStatus.PENDING);
    }

    private List<OrderDriverAssignment> openFor(StoreOrder o) {
        return offers.findByStoreOrderRecordIdAndAssignmentStatusAndDeletedFalse(o.getRecordId(), DriverAssignmentStatus.PENDING);
    }

    /** Freight orders can still get a driver until the driver is on the way. */
    private static boolean freightOpen(FreightOrder o) {
        return o.getOrderStatus() == OrderStatus.PLACED || o.getOrderStatus() == OrderStatus.DRIVER_REQUESTED
                || o.getOrderStatus() == OrderStatus.DRIVER_ASSIGNED;
    }

    /** Shop / food orders: from the moment the store accepted until it is handed over. */
    private static boolean storeOpen(StoreOrder o) {
        return o.getStatus() == StoreOrderStatus.ACCEPTED || o.getStatus() == StoreOrderStatus.PREPARING
                || o.getStatus() == StoreOrderStatus.READY;
    }

    private Set<Long> triedDrivers(List<OrderDriverAssignment> history) {
        return history.stream()
                .filter(h -> h.getAssignmentStatus() == DriverAssignmentStatus.REJECTED || h.getAssignmentStatus() == DriverAssignmentStatus.EXPIRED)
                .map(h -> h.getDriver().getRecordId()).collect(Collectors.toSet());
    }

    private int nextWave(List<OrderDriverAssignment> history) {
        return history.stream().map(OrderDriverAssignment::getWave).filter(Objects::nonNull).max(Integer::compare).orElse(0) + 1;
    }

    private Long driverId(String uuid) {
        return drivers.findByUuidAndDeletedFalse(uuid).map(Driver::getRecordId).orElse(-1L);
    }

    private static double[] storePoint(StoreOrder o) {
        var lat = o.getStoreType() == StoreType.SHOP ? o.getShop().getLatitude() : o.getRestaurant().getLatitude();
        var lng = o.getStoreType() == StoreType.SHOP ? o.getShop().getLongitude() : o.getRestaurant().getLongitude();
        return new double[]{lat.doubleValue(), lng.doubleValue()};
    }

    private void freightLog(FreightOrder o, String note, OrderStatus status) {
        FreightOrderStatusLog l = new FreightOrderStatusLog();
        l.setFreightOrder(o);
        l.setOrderStatus(status != null ? status : o.getOrderStatus());
        l.setRemarks(note);
        l.setStatusTime(LocalDateTime.now());
        freightLogs.save(l);
    }

    private void storeLog(StoreOrder o, String note, String by) {
        StoreOrderStatusLog l = new StoreOrderStatusLog();
        l.setOrder(o);
        l.setStatus(o.getStatus());
        l.setNote(note);
        l.setChangedBy(by);
        o.getLogs().add(l);
    }

    OfferDTO.View view(OrderDriverAssignment a) {
        OfferDTO.View v = new OfferDTO.View();
        v.setAssignmentUuid(a.getUuid());
        v.setExpiresAt(a.getExpiresAt());
        v.setSecondsLeft(a.getExpiresAt() == null ? 0 : Math.max(0, Duration.between(LocalDateTime.now(), a.getExpiresAt()).getSeconds()));
        v.setMode(a.getOfferMode());
        v.setStatus(a.getAssignmentStatus());
        v.setDistanceKm(a.getDistanceKm());
        if (a.getFreightOrder() != null) {
            FreightOrder o = a.getFreightOrder();
            v.setOrderType("FREIGHT");
            v.setOrderUuid(o.getUuid());
            v.setOrderNumber(o.getOrderNumber());
            v.setTitle(o.getOrderType() == null ? "Freight job" : o.getOrderType().name().replace('_', ' '));
            FreightOrderAddress p = o.getPickupAddress();
            if (p != null) {
                v.setPickupName(p.getAddressName());
                v.setPickupAddress(p.getFullAddress());
                v.setPickupLat(p.getLatitude());
                v.setPickupLng(p.getLongitude());
            }
            if (o.getDropAddresses() != null && !o.getDropAddresses().isEmpty()) {
                FreightOrderAddress d = o.getDropAddresses().get(o.getDropAddresses().size() - 1);
                v.setDropName(d.getAddressName());
                v.setDropAddress(d.getFullAddress());
                v.setDropLat(d.getLatitude());
                v.setDropLng(d.getLongitude());
                v.setItemCount(o.getDropAddresses().size());
            }
            v.setTripKm(o.getDistanceKm());
            v.setAmount(o.getTotalAmount());
        } else {
            StoreOrder o = a.getStoreOrder();
            v.setOrderType(o.getStoreType() == StoreType.SHOP ? "SHOP" : "FOOD");
            v.setOrderUuid(o.getUuid());
            v.setOrderNumber(o.getOrderNumber());
            v.setTitle(o.getStoreType() == StoreType.SHOP ? "Shop order" : "Food order");
            v.setPickupName(o.getStoreName());
            double[] at = storePoint(o);
            v.setPickupLat(at[0]);
            v.setPickupLng(at[1]);
            v.setPickupAddress(o.getStoreType() == StoreType.SHOP ? o.getShop().getAddress() : o.getRestaurant().getAddress());
            v.setDropName(o.getContactName());
            v.setDropAddress(o.getDeliveryAddress());
            v.setDropLat(o.getDeliveryLatitude());
            v.setDropLng(o.getDeliveryLongitude());
            v.setTripKm(o.getDistanceKm());
            v.setAmount(o.getTotalAmount());
            v.setItemCount(o.getItems() == null ? null : o.getItems().stream().mapToInt(i -> i.getQuantity() == null ? 1 : i.getQuantity()).sum());
            v.setPaymentMethod(o.getPaymentMethod() == null ? null : o.getPaymentMethod().name());
        }
        return v;
    }
}
