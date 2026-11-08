package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.StoreOrderDTO.*;
import com.bt.fretchain.primary.pojo.StoreOrder;
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

    @Transactional(readOnly = true)
    public DataTableResponsePacket<Card> list(StoreType type, Long storeId, PartnerListRequest req) {
        Specification<StoreOrder> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            p.add(cb.equal(root.get(field(type)).get("recordId"), storeId));
            if (req.getStatus() != null) {
                p.add(cb.equal(root.get("status"), req.getStatus()));
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
        boolean fifo = Boolean.TRUE.equals(req.getActive()) && req.getStatus() == null;
        Sort sort = Sort.by(fifo ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        return PaginationUtils.createResponse(orderRepository.findAll(spec,
                PageRequest.of(req.getPageNumber(), req.getPageSize(), sort)).map(orderService::toCard));
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
        if (next == StoreOrderStatus.REJECTED && (req.getReason() == null || req.getReason().isBlank())) {
            throw new BadRequestException("approval.reason.required");
        }
        o.setStatus(next);
        o.setStatusReason(next == StoreOrderStatus.REJECTED ? req.getReason().trim() : o.getStatusReason());
        o.setModifiedBy(actor.getRecordId());
        orderService.log(o, next, next == StoreOrderStatus.REJECTED ? req.getReason().trim() : null, actor.getFullName());
        if (next == StoreOrderStatus.REJECTED) {
            orderService.releaseOrder(o);       // stock and coupon go back
        }
        if (next == StoreOrderStatus.DELIVERED) {
            o.setPaymentPaid(true);             // cash collected on delivery
        }
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
