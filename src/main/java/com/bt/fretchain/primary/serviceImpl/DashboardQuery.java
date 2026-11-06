package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.DashboardDTO;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Small JPQL helpers shared by the dashboard builders (all read-only). */
@Component
public class DashboardQuery {

    /** Orders that are placed and not finished. */
    public static final List<OrderStatus> ACTIVE = List.of(
            OrderStatus.PLACED, OrderStatus.DRIVER_REQUESTED, OrderStatus.DRIVER_ASSIGNED, OrderStatus.ACCEPTED,
            OrderStatus.PICKUP_STARTED, OrderStatus.PICKUP_ARRIVED, OrderStatus.LOADING_STARTED,
            OrderStatus.LOADING_COMPLETED, OrderStatus.IN_TRANSIT, OrderStatus.DROP_ARRIVED,
            OrderStatus.UNLOADING_STARTED, OrderStatus.UNLOADING_COMPLETED);
    public static final List<OrderStatus> CANCELLED = List.of(
            OrderStatus.CANCELLED_BY_CUSTOMER, OrderStatus.CANCELLED_BY_DRIVER, OrderStatus.CANCELLED_BY_ADMIN);
    /** Not real orders yet. */
    public static final List<OrderStatus> DRAFTS = List.of(OrderStatus.DRAFT, OrderStatus.SAVED);

    @PersistenceContext
    private EntityManager em;

    private Query query(String jpql, Object... params) {
        Query q = em.createQuery(jpql);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q;
    }

    public long count(String jpql, Object... params) {
        Object r = query(jpql, params).getSingleResult();
        return r == null ? 0 : ((Number) r).longValue();
    }

    public BigDecimal sum(String jpql, Object... params) {
        Object r = query(jpql, params).getSingleResult();
        return r == null ? BigDecimal.ZERO : new BigDecimal(r.toString());
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> rows(String jpql, int max, Object... params) {
        Query q = query(jpql, params);
        if (max > 0) {
            q.setMaxResults(max);
        }
        return q.getResultList();
    }

    /** Rows of (key, value) as breakdown items. Keys are toString()'d (enums, names). */
    public List<Map<String, Object>> breakdown(String jpql, int max, Object... params) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] r : rows(jpql, max, params)) {
            items.add(DashboardDTO.item(r[0] == null ? "-" : r[0].toString(), (Number) r[1]));
        }
        return items;
    }

    /**
     * Daily series for the last `days` days (zero-filled).
     * jpql selects (date, value) and takes the start timestamp as ?1, then params.
     */
    public List<Map<String, Object>> daily(String jpql, int days, Object... params) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        Object[] all = new Object[params.length + 1];
        all[0] = from.atStartOfDay();
        System.arraycopy(params, 0, all, 1, params.length);
        Map<String, Number> byDay = new HashMap<>();
        for (Object[] r : rows(jpql, 0, all)) {
            byDay.put(String.valueOf(r[0]), (Number) r[1]);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            points.add(DashboardDTO.point(d.toString(), byDay.getOrDefault(d.toString(), 0)));
        }
        return points;
    }

    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    public static LocalDateTime startOfMonth() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    public static int clampDays(Integer days) {
        return days == null ? 14 : Math.max(7, Math.min(90, days));
    }
}
