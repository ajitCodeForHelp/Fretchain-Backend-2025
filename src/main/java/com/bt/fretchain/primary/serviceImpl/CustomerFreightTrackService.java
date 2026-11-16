package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.FreightTrackDTO.*;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.AddressRole;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** The customer's freight orders: list (in progress / past) and the tracking view. */
@Service
@RequiredArgsConstructor
public class CustomerFreightTrackService {

    /** Statuses that count as "in progress" (drafts are not orders yet). */
    static final Set<OrderStatus> ACTIVE = EnumSet.of(
            OrderStatus.PLACED, OrderStatus.DRIVER_REQUESTED, OrderStatus.DRIVER_ASSIGNED, OrderStatus.ACCEPTED,
            OrderStatus.PICKUP_STARTED, OrderStatus.PICKUP_ARRIVED, OrderStatus.LOADING_STARTED, OrderStatus.LOADING_COMPLETED,
            OrderStatus.IN_TRANSIT, OrderStatus.DROP_ARRIVED, OrderStatus.UNLOADING_STARTED, OrderStatus.UNLOADING_COMPLETED);
    private static final Set<OrderStatus> PAST = EnumSet.of(
            OrderStatus.DELIVERED, OrderStatus.CANCELLED_BY_CUSTOMER, OrderStatus.CANCELLED_BY_DRIVER, OrderStatus.CANCELLED_BY_ADMIN);

    private static final String[] STEPS = {"Order placed", "Driver assigned", "Pickup", "Loading", "On the way", "Delivered"};

    private final FreightOrderRepository orders;
    private final FreightOrderAddressRepository addresses;
    private final FreightOrderStatusLogRepository logs;
    private final OfferService offers;
    private final OrderNotifier notifier;
    private final ProofService proofService;

    @Transactional(readOnly = true)
    public DataTableResponsePacket<Card> list(ListRequest req, SystemUser customer) {
        Specification<FreightOrder> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            p.add(cb.equal(root.get("customer").get("recordId"), customer.getRecordId()));
            Predicate active = root.get("orderStatus").in(ACTIVE);
            Predicate past = root.get("orderStatus").in(PAST);
            if (req.getActive() == null) p.add(cb.or(active, past));
            else p.add(req.getActive() ? active : past);
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PaginationUtils.createResponse(orders.findAll(spec,
                PageRequest.of(req.getPageNumber(), req.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::card));
    }

    @Transactional(readOnly = true)
    public Track track(String uuid, SystemUser customer) {
        FreightOrder o = own(uuid, customer);
        Track t = new Track();
        fill(t, o);
        List<FreightOrderAddress> drops = dropsOf(o);
        int cur = o.getCurrentDropIndex() == null ? 0 : o.getCurrentDropIndex();
        t.setCurrentDrop(cur);
        if (o.getPickupAddress() != null) t.setPickup(stop(o.getPickupAddress(), 0, false));
        for (int i = 0; i < drops.size(); i++) {
            // a drop is done once the driver moved on from it (or the order is delivered)
            boolean done = o.getOrderStatus() == OrderStatus.DELIVERED || i < cur
                    || (i == cur && o.getOrderStatus() == OrderStatus.UNLOADING_COMPLETED && cur == drops.size() - 1);
            t.getDrops().add(stop(drops.get(i), i + 1, done));
        }
        t.setDistanceKm(o.getDistanceKm());
        if (o.getDriver() != null) t.setDriverPhotoUrl(o.getDriver().getProfileImageUrl());
        t.setSteps(steps(o));
        for (FreightOrderStatusLog l : logs.findByFreightOrderOrderByStatusTimeAsc(o)) {
            Event e = new Event();
            e.setStatus(l.getOrderStatus().name());
            e.setLabel(label(l.getOrderStatus(), 0, 1));
            // internal notes such as "Offered to <driver>" stay inside the company
            String note = l.getRemarks();
            e.setNote(note != null && (note.startsWith("Offered") || note.startsWith("Driver:") || note.contains("declined")
                    || note.contains("in time") || note.startsWith("Replaced")) ? null : note);
            e.setAt(l.getStatusTime() != null ? l.getStatusTime() : l.getCreatedAt());
            t.getTimeline().add(e);
        }
        t.setCancellable(o.getDriver() == null && (o.getOrderStatus() == OrderStatus.PLACED || o.getOrderStatus() == OrderStatus.DRIVER_REQUESTED));
        // the driver's position is shared with the customer only while the order is on the road
        if (o.getDriver() != null && o.getOrderStatus() != OrderStatus.DELIVERED && o.getDriver().getCurrentLat() != null
                && o.getOrderStatus().ordinal() >= OrderStatus.PICKUP_STARTED.ordinal()) {
            t.setDriverLat(o.getDriver().getCurrentLat());
            t.setDriverLng(o.getDriver().getCurrentLng());
            t.setDriverLocationAt(o.getDriver().getLastLocationUpdatedAt());
        }
        for (var pv : proofService.forOrder(o)) {
            pv.setSignatureSvg(null);
            t.getProofs().add(pv);
        }
        return t;
    }

    /** The customer cancels an order that has no driver yet. */
    @Transactional
    public Track cancel(String uuid, String reason, SystemUser customer) {
        FreightOrder o = own(uuid, customer);
        if (o.getDriver() != null || !(o.getOrderStatus() == OrderStatus.PLACED || o.getOrderStatus() == OrderStatus.DRIVER_REQUESTED)) {
            throw new BadRequestException("freight.cancel.notallowed");
        }
        offers.cancelOpen(o, "Order cancelled by the customer");
        o.setOrderStatus(OrderStatus.CANCELLED_BY_CUSTOMER);
        o.setModifiedBy(customer.getRecordId());
        orders.save(o);
        FreightOrderStatusLog l = new FreightOrderStatusLog();
        l.setFreightOrder(o);
        l.setOrderStatus(OrderStatus.CANCELLED_BY_CUSTOMER);
        l.setRemarks(reason == null || reason.isBlank() ? "Cancelled by the customer" : reason.trim());
        l.setUpdatedBy(customer);
        l.setStatusTime(LocalDateTime.now());
        logs.save(l);
        return track(uuid, customer);
    }

    // ---------------------------------------------------------------- mapping

    private Card card(FreightOrder o) {
        Card c = new Card();
        fill(c, o);
        return c;
    }

    private void fill(Card c, FreightOrder o) {
        List<FreightOrderAddress> drops = dropsOf(o);
        int n = Math.max(drops.size(), 1);
        int cur = Math.min(o.getCurrentDropIndex() == null ? 0 : o.getCurrentDropIndex(), n - 1);
        c.setUuid(o.getUuid());
        c.setOrderNumber(o.getOrderNumber());
        c.setOrderType(o.getOrderType());
        c.setStatus(o.getOrderStatus().name());
        c.setStatusLabel(label(o.getOrderStatus(), cur, n));
        c.setActive(ACTIVE.contains(o.getOrderStatus()));
        c.setPickupAddress(o.getPickupAddress() == null ? null : o.getPickupAddress().getFullAddress());
        c.setDropAddress(drops.isEmpty() ? null : drops.get(0).getFullAddress());
        c.setDropCount(drops.size());
        c.setVehicleCategory(o.getVehicleCategory() == null ? null : o.getVehicleCategory().getNameEn());
        c.setTotalAmount(o.getTotalAmount());
        c.setCreatedAt(o.getCreatedAt());
        Driver d = o.getDriver();
        if (d != null) {
            c.setDriverName(d.getFullName());
            c.setDriverMobile((d.getCountryCode() == null ? "" : d.getCountryCode()) + d.getMobile());
            c.setDriverVehicle(d.getVehicle() == null ? null : d.getVehicle().getVehicleNumber());
        }
    }

    /** In words, for the customer. */
    static String label(OrderStatus s, int drop, int drops) {
        String of = drops > 1 ? " (drop " + (drop + 1) + " of " + drops + ")" : "";
        return switch (s) {
            case DRAFT, SAVED -> "Draft";
            case PLACED, DRIVER_REQUESTED -> "Finding a driver";
            case DRIVER_ASSIGNED, ACCEPTED -> "Driver assigned";
            case PICKUP_STARTED -> "Driver on the way to pickup";
            case PICKUP_ARRIVED -> "Driver at the pickup";
            case LOADING_STARTED -> "Loading";
            case LOADING_COMPLETED -> "Loaded";
            case IN_TRANSIT -> "On the way" + of;
            case DROP_ARRIVED -> "Arrived at the drop" + of;
            case UNLOADING_STARTED -> "Unloading" + of;
            case UNLOADING_COMPLETED -> "Unloaded" + of;
            case DELIVERED -> "Delivered";
            case CANCELLED_BY_CUSTOMER -> "Cancelled by you";
            case CANCELLED_BY_DRIVER, CANCELLED_BY_ADMIN -> "Cancelled";
        };
    }

    private List<Step> steps(FreightOrder o) {
        int pos = switch (o.getOrderStatus()) {
            case PLACED, DRIVER_REQUESTED -> 0;
            case DRIVER_ASSIGNED, ACCEPTED -> 1;
            case PICKUP_STARTED, PICKUP_ARRIVED -> 2;
            case LOADING_STARTED, LOADING_COMPLETED -> 3;
            case IN_TRANSIT, DROP_ARRIVED, UNLOADING_STARTED, UNLOADING_COMPLETED -> 4;
            case DELIVERED -> 5;
            default -> -1;
        };
        List<Step> list = new ArrayList<>();
        for (int i = 0; i < STEPS.length; i++) {
            Step s = new Step();
            s.setKey(String.valueOf(i));
            s.setLabel(STEPS[i]);
            s.setDone(pos >= 0 && i <= pos);
            s.setCurrent(pos >= 0 && i == pos);
            list.add(s);
        }
        return list;
    }

    private Stop stop(FreightOrderAddress a, int sequence, boolean done) {
        Stop s = new Stop();
        s.setSequence(sequence);
        s.setName(a.getAddressName());
        s.setAddress(a.getFullAddress());
        s.setContactName(a.getContactName());
        s.setContactPhone(a.getContactNumber());
        s.setLat(a.getLatitude());
        s.setLng(a.getLongitude());
        s.setDone(done);
        return s;
    }

    private List<FreightOrderAddress> dropsOf(FreightOrder o) {
        return addresses.findByFreightOrderOrderBySequenceNoAsc(o).stream()
                .filter(x -> x.getAddressRole() == AddressRole.DROP).collect(Collectors.toList());
    }

    private FreightOrder own(String uuid, SystemUser customer) {
        return orders.findByUuidAndCustomerAndDeletedFalse(uuid, customer)
                .filter(o -> o.getOrderStatus() != OrderStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("order.notfound"));
    }
}
