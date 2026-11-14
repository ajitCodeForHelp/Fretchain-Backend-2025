package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.OfferDTO;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.*;
import com.bt.fretchain.primary.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** The driver moves an accepted order forward, step by step; the customer is told after each step. */
@Service
@RequiredArgsConstructor
public class DriverOrderService {

    private final OrderDriverAssignmentRepository assignments;
    private final DriverRepository drivers;
    private final StoreOrderRepository storeOrders;
    private final FreightOrderRepository freightOrders;
    private final FreightOrderStatusLogRepository freightLogs;
    private final OfferService offers;
    private final OrderNotifier notifier;

    /** Presses the driver's next button. `expectedStep` guards against a double tap / stale screen. */
    @Transactional
    public OfferDTO.View advance(Driver driver, String orderUuid, String expectedStep) {
        StoreOrder so = storeOrders.findByUuidAndDeletedFalse(orderUuid).orElse(null);
        if (so != null) {
            mine(so.getDriver(), driver);
            advanceStore(so, driver, expectedStep);
        } else {
            FreightOrder fo = freightOrders.findByUuidAndDeletedFalse(orderUuid).orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
            mine(fo.getDriver(), driver);
            advanceFreight(fo, driver, expectedStep);
        }
        return get(driver, orderUuid);
    }

    @Transactional(readOnly = true)
    public OfferDTO.View get(Driver driver, String orderUuid) {
        return acceptedFor(driver).stream()
                .filter(a -> orderUuid.equals(a.getFreightOrder() != null ? a.getFreightOrder().getUuid() : a.getStoreOrder().getUuid()))
                .findFirst().map(offers::view).orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    /** Orders this driver finished or that ended (newest first). */
    @Transactional(readOnly = true)
    public List<OfferDTO.View> history(Driver driver, int limit) {
        return acceptedFor(driver).stream()
                .filter(a -> a.getFreightOrder() != null ? !live(a.getFreightOrder()) : !a.getStoreOrder().getStatus().isActive())
                .limit(limit).map(offers::view).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- shop / food

    private void advanceStore(StoreOrder o, Driver driver, String expected) {
        int pos = DriverProgress.storePosition(o);
        if (!o.getStatus().isActive() || pos >= 4) {
            throw new BadRequestException("driver.order.finished");
        }
        OfferDTO.View probe = new OfferDTO.View();
        DriverProgress.fill(probe, o);
        OfferDTO.NextStep next = probe.getNext();
        if (next == null) throw new BadRequestException("driver.order.finished");
        if (expected != null && !expected.equals(next.getKey())) throw new BadRequestException("driver.order.step.changed");
        if (!next.isEnabled()) throw new BadRequestException("driver.order.not.ready");

        String name = driver.getFullName();
        switch (next.getKey()) {
            case "ARRIVED_STORE" -> {
                o.setDriverStep(DriverProgress.ARRIVED_STORE);
                log(o, name + " arrived at the store", name);
            }
            case "PICKED_UP" -> {
                o.setStatus(StoreOrderStatus.OUT_FOR_DELIVERY);
                log(o, "Picked up by " + name, name);
                notifier.store(o, StoreOrderStatus.OUT_FOR_DELIVERY, null);
            }
            case "ARRIVED_CUSTOMER" -> {
                o.setDriverStep(DriverProgress.ARRIVED_CUSTOMER);
                log(o, name + " arrived at the customer", name);
                notifier.storeText(o, name + " has arrived at your address");
            }
            default -> {   // DELIVERED
                o.setStatus(StoreOrderStatus.DELIVERED);
                o.setPaymentPaid(true);
                driver.setAvailable(true);
                drivers.save(driver);
                log(o, "Delivered by " + name, name);
                notifier.store(o, StoreOrderStatus.DELIVERED, null);
            }
        }
        o.setModifiedBy(driver.getRecordId());
        storeOrders.save(o);
    }

    private void log(StoreOrder o, String note, String by) {
        StoreOrderStatusLog l = new StoreOrderStatusLog();
        l.setOrder(o);
        l.setStatus(o.getStatus());
        l.setNote(note);
        l.setChangedBy(by);
        o.getLogs().add(l);
    }

    // ---------------------------------------------------------------- freight

    private void advanceFreight(FreightOrder o, Driver driver, String expected) {
        OrderStatus next = DriverProgress.freightNextStatus(o);
        if (next == null) throw new BadRequestException("driver.order.finished");
        if (expected != null && !expected.equals(next.name())) throw new BadRequestException("driver.order.step.changed");
        o.setOrderStatus(next);
        o.setModifiedBy(driver.getRecordId());
        freightOrders.save(o);

        FreightOrderStatusLog l = new FreightOrderStatusLog();
        l.setFreightOrder(o);
        l.setOrderStatus(next);
        l.setRemarks("Driver: " + driver.getFullName());
        l.setStatusTime(LocalDateTime.now());
        freightLogs.save(l);

        if (next == OrderStatus.DELIVERED) {
            driver.setAvailable(true);
            drivers.save(driver);
        }
        notifier.freight(o, next);
    }

    // ---------------------------------------------------------------- helpers

    private List<OrderDriverAssignment> acceptedFor(Driver driver) {
        return assignments.findByDriverRecordIdAndAssignmentStatusAndDeletedFalseOrderByRespondedAtDesc(driver.getRecordId(), DriverAssignmentStatus.ACCEPTED)
                .stream()
                // an order that was re-assigned to someone else is not this driver's any more
                .filter(a -> a.getFreightOrder() != null
                        ? a.getFreightOrder().getDriver() != null && Objects.equals(a.getFreightOrder().getDriver().getRecordId(), driver.getRecordId())
                        : a.getStoreOrder().getDriver() != null && Objects.equals(a.getStoreOrder().getDriver().getRecordId(), driver.getRecordId()))
                .collect(Collectors.toList());
    }

    private static boolean live(FreightOrder o) {
        return o.getOrderStatus() != OrderStatus.DELIVERED && o.getOrderStatus() != OrderStatus.CANCELLED_BY_ADMIN
                && o.getOrderStatus() != OrderStatus.CANCELLED_BY_CUSTOMER && o.getOrderStatus() != OrderStatus.CANCELLED_BY_DRIVER;
    }

    private static void mine(Driver assigned, Driver driver) {
        if (assigned == null || !assigned.getRecordId().equals(driver.getRecordId())) {
            throw new ResourceNotFoundException("record.notfound");
        }
    }
}
