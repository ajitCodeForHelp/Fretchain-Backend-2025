package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.StoreOrderDTO;
import com.bt.fretchain.primary.dto.StoreOrderDTO.*;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.*;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.utils.GeoUtils;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Customer side of shop and food orders: price a cart (with coupon), place the order, track it, cancel it.
 *
 * Browsing (nearby, search, menu) is public; pricing with a coupon, placing and tracking need a customer login.
 * The server re-prices every cart, so the app never decides prices, stock or discounts.
 */
@Service
@RequiredArgsConstructor
public class StoreOrderService {

    private final StoreOrderRepository orderRepository;
    private final ShopMasterRepository shopRepository;
    private final RestaurantMasterRepository restaurantRepository;
    private final ShopItemRepository shopItemRepository;
    private final RestaurantItemRepository restaurantItemRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /** Time zone used for "open now". Empty = server default. */
    @Value("${marketplace.time-zone:}")
    private String timeZone;

    // ================================================================ pricing

    /** What the cart resolves to: the response plus the entities needed to place the order. */
    private static final class Resolved {
        Quote quote = new Quote();
        ShopMaster shop;
        RestaurantMaster restaurant;
        Coupon coupon;
        final List<ResolvedLine> lines = new ArrayList<>();
    }

    private static final class ResolvedLine {
        final Line line;
        final ShopItem shopItem;
        final RestaurantItem restaurantItem;

        ResolvedLine(Line line, ShopItem shopItem, RestaurantItem restaurantItem) {
            this.line = line;
            this.shopItem = shopItem;
            this.restaurantItem = restaurantItem;
        }
    }

    /** Store facts used for pricing, for both store types. */
    private record StoreInfo(String name, String logoUrl, String mobile, BigDecimal fee, BigDecimal minOrder,
                             BigDecimal radiusKm, BigDecimal lat, BigDecimal lng, Boolean accepting,
                             LocalTime open, LocalTime close, Integer prepMinutes) {
    }

    @Transactional(readOnly = true)
    public Quote quote(QuoteRequest req, SystemUser customer) {
        requireCustomer(customer);
        return resolve(req, customer).quote;
    }

    private Resolved resolve(QuoteRequest req, SystemUser customer) {
        Resolved r = new Resolved();
        Quote q = r.quote;
        q.setStoreType(req.getStoreType());
        q.setStoreUuid(req.getStoreUuid());

        StoreInfo store = loadStore(req, r);
        if (store == null) {
            q.getIssues().add(new Issue("STORE_UNAVAILABLE", null, null, null));
            return r;
        }
        q.setStoreName(store.name());
        q.setMinOrderAmount(store.minOrder());

        // ---- cart lines (the same item twice is one line)
        Map<String, Integer> wanted = new LinkedHashMap<>();
        for (CartLine l : req.getItems()) {
            wanted.merge(l.getItemUuid(), l.getQuantity(), Integer::sum);
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> e : wanted.entrySet()) {
            ResolvedLine rl = resolveLine(req.getStoreType(), r, e.getKey(), Math.min(e.getValue(), 50), q.getIssues());
            if (rl != null) {
                r.lines.add(rl);
                q.getLines().add(rl.line);
                subtotal = subtotal.add(rl.line.getLineTotal());
            }
        }
        q.setSubtotal(subtotal);
        q.setDeliveryFee(store.fee() == null ? BigDecimal.ZERO : store.fee());

        // ---- store rules: open, delivery area, minimum order
        if (!isOpenNow(store.accepting(), store.open(), store.close(), now())) {
            q.getIssues().add(new Issue("STORE_CLOSED", null, null, null));
        }
        if (store.lat() != null && store.lng() != null) {
            double km = GeoUtils.round2(GeoUtils.haversineKm(req.getLatitude(), req.getLongitude(),
                    store.lat().doubleValue(), store.lng().doubleValue()));
            q.setDistanceKm(km);
            q.setEstimatedMinutes(GeoUtils.estimateDeliveryMinutes(store.prepMinutes(), km));
            if (store.radiusKm() != null && km > store.radiusKm().doubleValue()) {
                q.getIssues().add(new Issue("OUT_OF_AREA", null, null, store.radiusKm()));
            }
        }
        if (store.minOrder() != null && subtotal.compareTo(store.minOrder()) < 0) {
            q.getIssues().add(new Issue("MIN_ORDER", null, null, store.minOrder()));
        }

        // ---- coupon
        BigDecimal discount = BigDecimal.ZERO;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            Object[] result = applyCoupon(req.getCouponCode().trim(), subtotal, customer);
            if (result[0] instanceof Issue issue) {
                q.setCouponIssue(issue);
            } else {
                r.coupon = (Coupon) result[0];
                discount = (BigDecimal) result[1];
                q.setCouponCode(r.coupon.getCouponCode());
            }
        }
        q.setDiscountAmount(discount);
        q.setTotalAmount(subtotal.add(q.getDeliveryFee()).subtract(discount).max(BigDecimal.ZERO));
        q.setCanPlace(q.getIssues().isEmpty() && !r.lines.isEmpty());
        return r;
    }

    private StoreInfo loadStore(QuoteRequest req, Resolved r) {
        if (req.getStoreType() == StoreType.SHOP) {
            ShopMaster s = shopRepository.findByUuidAndDeletedFalse(req.getStoreUuid()).orElse(null);
            if (s == null || !s.isActive() || s.getStatus() != ShopStatus.APPROVED) {
                return null;
            }
            r.shop = s;
            return new StoreInfo(s.getNameEn(), s.getLogoUrl(), s.getMobile(), s.getDeliveryFee(), s.getMinOrderAmount(),
                    s.getDeliveryRadiusKm(), s.getLatitude(), s.getLongitude(), s.getAcceptingOrders(),
                    s.getOpeningTime(), s.getClosingTime(), s.getAvgPreparationMinutes());
        }
        RestaurantMaster s = restaurantRepository.findByUuidAndDeletedFalse(req.getStoreUuid()).orElse(null);
        if (s == null || !s.isActive() || s.getStatus() != RestaurantStatus.APPROVED) {
            return null;
        }
        r.restaurant = s;
        return new StoreInfo(s.getNameEn(), s.getLogoUrl(), s.getMobile(), s.getDeliveryFee(), s.getMinOrderAmount(),
                s.getDeliveryRadiusKm(), s.getLatitude(), s.getLongitude(), s.getAcceptingOrders(),
                s.getOpeningTime(), s.getClosingTime(), s.getAvgPreparationMinutes());
    }

    private ResolvedLine resolveLine(StoreType type, Resolved r, String itemUuid, int qty, List<Issue> issues) {
        Line line = new Line();
        line.setItemUuid(itemUuid);
        line.setQuantity(qty);

        if (type == StoreType.SHOP) {
            ShopItem item = shopItemRepository.findByUuidAndDeletedFalse(itemUuid).orElse(null);
            if (item == null || !item.getShop().getRecordId().equals(r.shop.getRecordId())) {
                issues.add(new Issue("ITEM_NOT_FOUND", itemUuid, null, null));
                return null;
            }
            fillLine(line, item.getNameEn(), item.getImageUrl(), item.getPrice(), item.getDiscountPrice(), qty);
            boolean visible = item.isActive() && Boolean.TRUE.equals(item.getAvailable())
                    && item.getCategory().isActive() && !item.getCategory().isDeleted()
                    && (item.getSubCategory() == null || (item.getSubCategory().isActive() && !item.getSubCategory().isDeleted()));
            int stock = item.getStockQuantity() == null ? 0 : item.getStockQuantity();
            if (!visible || stock <= 0) {
                issues.add(new Issue("ITEM_UNAVAILABLE", itemUuid, item.getNameEn(), null));
            } else if (stock < qty) {
                issues.add(new Issue("ITEM_STOCK", itemUuid, item.getNameEn(), BigDecimal.valueOf(stock)));
            }
            return new ResolvedLine(line, item, null);
        }

        RestaurantItem item = restaurantItemRepository.findByUuidAndDeletedFalse(itemUuid).orElse(null);
        if (item == null || !item.getRestaurant().getRecordId().equals(r.restaurant.getRecordId())) {
            issues.add(new Issue("ITEM_NOT_FOUND", itemUuid, null, null));
            return null;
        }
        fillLine(line, item.getNameEn(), item.getImageUrl(), item.getPrice(), item.getDiscountPrice(), qty);
        boolean visible = item.isActive() && Boolean.TRUE.equals(item.getAvailable())
                && item.getCategory().isActive() && !item.getCategory().isDeleted()
                && (item.getSubCategory() == null || (item.getSubCategory().isActive() && !item.getSubCategory().isDeleted()));
        if (!visible) {
            issues.add(new Issue("ITEM_UNAVAILABLE", itemUuid, item.getNameEn(), null));
        }
        return new ResolvedLine(line, null, item);
    }

    private static void fillLine(Line line, String name, String image, BigDecimal price, BigDecimal discount, int qty) {
        BigDecimal unit = discount != null ? discount : price;
        line.setItemName(name);
        line.setImageUrl(image);
        line.setUnitPrice(unit);
        line.setOriginalPrice(discount != null && price != null && discount.compareTo(price) < 0 ? price : null);
        line.setLineTotal(unit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP));
    }

    /** Returns {Issue} when the coupon cannot be used, or {Coupon, discountAmount}. */
    private Object[] applyCoupon(String code, BigDecimal subtotal, SystemUser customer) {
        Coupon c = couponRepository.findByCouponCodeIgnoreCaseAndDeletedFalse(code).orElse(null);
        if (c == null) {
            return new Object[]{new Issue("COUPON_NOT_FOUND", null, null, null)};
        }
        LocalDateTime now = LocalDateTime.now();
        if (!c.isActive()) {
            return new Object[]{new Issue("COUPON_INACTIVE", null, null, null)};
        }
        if (c.getStartDate() != null && c.getStartDate().isAfter(now)) {
            return new Object[]{new Issue("COUPON_NOT_STARTED", null, null, null)};
        }
        if (c.getEndDate() != null && c.getEndDate().isBefore(now)) {
            return new Object[]{new Issue("COUPON_EXPIRED", null, null, null)};
        }
        if (c.getMinimumOrderAmount() != null && subtotal.compareTo(c.getMinimumOrderAmount()) < 0) {
            return new Object[]{new Issue("COUPON_MIN_ORDER", null, null, c.getMinimumOrderAmount())};
        }
        if (c.getUsageLimit() != null && c.getUsedCount() != null && c.getUsedCount() >= c.getUsageLimit()) {
            return new Object[]{new Issue("COUPON_USED_UP", null, null, null)};
        }
        if (c.getPerCustomerLimit() != null && couponUsageRepository
                .countByCoupon_RecordIdAndCustomer_RecordIdAndDeletedFalse(c.getRecordId(), customer.getRecordId()) >= c.getPerCustomerLimit()) {
            return new Object[]{new Issue("COUPON_LIMIT_REACHED", null, null, null)};
        }
        BigDecimal discount;
        if (c.getDiscountType() == CouponDiscountType.PERCENTAGE) {
            discount = subtotal.multiply(c.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (c.getMaxDiscountAmount() != null) {
                discount = discount.min(c.getMaxDiscountAmount());
            }
        } else {
            discount = c.getDiscountValue();
        }
        discount = discount.min(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new Object[]{c, discount};
    }

    /** Coupons the customer could use now (for the "apply coupon" screen). */
    @Transactional(readOnly = true)
    public List<CouponInfo> availableCoupons(SystemUser customer) {
        requireCustomer(customer);
        LocalDateTime now = LocalDateTime.now();
        List<CouponInfo> result = new ArrayList<>();
        for (Coupon c : couponRepository.findAll()) {
            if (c.isDeleted() || !c.isActive()
                    || (c.getStartDate() != null && c.getStartDate().isAfter(now))
                    || (c.getEndDate() != null && c.getEndDate().isBefore(now))
                    || (c.getUsageLimit() != null && c.getUsedCount() != null && c.getUsedCount() >= c.getUsageLimit())) {
                continue;
            }
            if (c.getPerCustomerLimit() != null && couponUsageRepository
                    .countByCoupon_RecordIdAndCustomer_RecordIdAndDeletedFalse(c.getRecordId(), customer.getRecordId()) >= c.getPerCustomerLimit()) {
                continue;
            }
            CouponInfo i = new CouponInfo();
            i.setCode(c.getCouponCode());
            i.setTitle(c.getTitleEn());
            i.setDescription(c.getDescriptionEn());
            i.setDiscountType(c.getDiscountType() == null ? null : c.getDiscountType().name());
            i.setDiscountValue(c.getDiscountValue());
            i.setMaxDiscountAmount(c.getMaxDiscountAmount());
            i.setMinimumOrderAmount(c.getMinimumOrderAmount());
            i.setValidUntil(c.getEndDate());
            result.add(i);
        }
        return result;
    }

    // ================================================================ place / track / cancel

    @Transactional
    public Detail place(PlaceRequest req, SystemUser customer) {
        requireCustomer(customer);
        Resolved r = resolve(req, customer);
        if (!r.quote.isCanPlace()) {
            throw new BadRequestException("storeorder.cart.invalid");
        }
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank() && r.coupon == null) {
            throw new BadRequestException("storeorder.coupon.invalid");
        }

        StoreOrder o = new StoreOrder();
        o.setStoreType(req.getStoreType());
        o.setShop(r.shop);
        o.setRestaurant(r.restaurant);
        o.setStoreName(r.quote.getStoreName());
        o.setCustomer(customer);
        o.setStatus(StoreOrderStatus.PLACED);
        o.setDeliveryAddress(req.getDeliveryAddress().trim());
        o.setDeliveryLatitude(req.getLatitude());
        o.setDeliveryLongitude(req.getLongitude());
        o.setContactName(req.getContactName().trim());
        o.setContactMobile(req.getContactMobile().trim());
        o.setInstructions(blankToNull(req.getInstructions()));
        o.setDistanceKm(r.quote.getDistanceKm());
        o.setEstimatedMinutes(r.quote.getEstimatedMinutes());
        o.setSubtotal(r.quote.getSubtotal());
        o.setDeliveryFee(r.quote.getDeliveryFee());
        o.setDiscountAmount(r.quote.getDiscountAmount());
        o.setTotalAmount(r.quote.getTotalAmount());
        o.setCouponCode(r.quote.getCouponCode());
        o.setPaymentMethod(req.getPaymentMethod() == null ? StorePaymentMethod.CASH_ON_DELIVERY : req.getPaymentMethod());
        o.setPaymentPaid(false);
        o.setCreatedBy(customer.getRecordId());
        o.setModifiedBy(customer.getRecordId());

        for (ResolvedLine rl : r.lines) {
            // shop stock is re-checked under a row lock and reduced
            if (rl.shopItem != null) {
                ShopItem locked = shopItemRepository.findWithLockByUuidAndDeletedFalse(rl.line.getItemUuid())
                        .orElseThrow(() -> new BadRequestException("storeorder.cart.invalid"));
                int stock = locked.getStockQuantity() == null ? 0 : locked.getStockQuantity();
                if (stock < rl.line.getQuantity()) {
                    throw new BadRequestException("storeorder.cart.invalid");
                }
                locked.setStockQuantity(stock - rl.line.getQuantity());
                shopItemRepository.save(locked);
            }
            StoreOrderItem item = new StoreOrderItem();
            item.setOrder(o);
            item.setItemUuid(rl.line.getItemUuid());
            item.setItemName(rl.line.getItemName());
            item.setImageUrl(rl.line.getImageUrl());
            item.setUnitPrice(rl.line.getUnitPrice());
            item.setQuantity(rl.line.getQuantity());
            item.setLineTotal(rl.line.getLineTotal());
            o.getItems().add(item);
        }
        log(o, StoreOrderStatus.PLACED, null, customer.getFullName());
        orderRepository.save(o);
        o.setOrderNumber((req.getStoreType() == StoreType.SHOP ? "SHP" : "FOD")
                + LocalDate.now().toString().replace("-", "") + String.format("%05d", o.getRecordId()));
        orderRepository.save(o);

        if (r.coupon != null) {
            r.coupon.setUsedCount((r.coupon.getUsedCount() == null ? 0 : r.coupon.getUsedCount()) + 1);
            couponRepository.save(r.coupon);
            CouponUsage u = new CouponUsage();
            u.setCoupon(r.coupon);
            u.setCustomer(customer);
            u.setStoreOrder(o);
            u.setDiscountAmount(o.getDiscountAmount());
            couponUsageRepository.save(u);
        }
        return toDetail(o);
    }

    @Transactional(readOnly = true)
    public DataTableResponsePacket<Card> myOrders(ListRequest req, SystemUser customer) {
        requireCustomer(customer);
        Specification<StoreOrder> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            p.add(cb.equal(root.get("customer").get("recordId"), customer.getRecordId()));
            if (req.getActive() != null) {
                Predicate active = root.get("status").in(StoreOrderStatus.ACTIVE);
                p.add(req.getActive() ? active : cb.not(active));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PaginationUtils.createResponse(orderRepository.findAll(spec,
                PageRequest.of(req.getPageNumber(), req.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toCard));
    }

    @Transactional(readOnly = true)
    public Detail myOrder(String uuid, SystemUser customer) {
        requireCustomer(customer);
        Detail d = toDetail(ownOrder(uuid, customer));
        d.setNextStatuses(List.of());
        return d;
    }

    @Transactional
    public Detail cancel(String uuid, String reason, SystemUser customer) {
        requireCustomer(customer);
        StoreOrder o = ownOrder(uuid, customer);
        if (o.getStatus() != StoreOrderStatus.PLACED) {
            throw new BadRequestException("storeorder.cancel.notallowed");
        }
        o.setStatus(StoreOrderStatus.CANCELLED);
        o.setStatusReason(blankToNull(reason));
        o.setModifiedBy(customer.getRecordId());
        log(o, StoreOrderStatus.CANCELLED, blankToNull(reason), customer.getFullName());
        releaseOrder(o);
        return toDetail(orderRepository.save(o));
    }

    /** Gives back shop stock and the coupon use of an order that will not be delivered. */
    void releaseOrder(StoreOrder o) {
        for (StoreOrderItem item : o.getItems()) {
            if (o.getStoreType() == StoreType.SHOP && item.getItemUuid() != null) {
                shopItemRepository.findWithLockByUuidAndDeletedFalse(item.getItemUuid()).ifPresent(si -> {
                    si.setStockQuantity((si.getStockQuantity() == null ? 0 : si.getStockQuantity()) + item.getQuantity());
                    shopItemRepository.save(si);
                });
            }
        }
        for (CouponUsage u : couponUsageRepository.findByStoreOrder_RecordIdAndDeletedFalse(o.getRecordId())) {
            u.setDeleted(true);
            couponUsageRepository.save(u);
            Coupon c = u.getCoupon();
            c.setUsedCount(Math.max(0, (c.getUsedCount() == null ? 0 : c.getUsedCount()) - 1));
            couponRepository.save(c);
        }
    }

    // ================================================================ mapping (shared with the store side)

    void log(StoreOrder o, StoreOrderStatus status, String note, String by) {
        StoreOrderStatusLog l = new StoreOrderStatusLog();
        l.setOrder(o);
        l.setStatus(status);
        l.setNote(note);
        l.setChangedBy(by);
        o.getLogs().add(l);
    }

    Card toCard(StoreOrder o) {
        Card c = new Card();
        c.setUuid(o.getUuid());
        c.setOrderNumber(o.getOrderNumber());
        c.setStoreType(o.getStoreType());
        c.setStoreName(o.getStoreName());
        c.setStoreLogoUrl(logoOf(o));
        c.setStatus(o.getStatus());
        c.setTotalAmount(o.getTotalAmount());
        c.setItemCount(o.getItems().stream().mapToInt(StoreOrderItem::getQuantity).sum());
        c.setItemsSummary(o.getItems().stream().limit(3)
                .map(i -> i.getQuantity() + " x " + i.getItemName()).reduce((a, b) -> a + ", " + b).orElse("")
                + (o.getItems().size() > 3 ? " +" + (o.getItems().size() - 3) : ""));
        c.setCustomerName(o.getContactName());
        c.setContactMobile(o.getContactMobile());
        c.setCreatedAt(o.getCreatedAt());
        return c;
    }

    Detail toDetail(StoreOrder o) {
        Detail d = new Detail();
        d.setUuid(o.getUuid());
        d.setOrderNumber(o.getOrderNumber());
        d.setStoreType(o.getStoreType());
        d.setStoreUuid(o.storeUuid());
        d.setStoreName(o.getStoreName());
        d.setStoreLogoUrl(logoOf(o));
        d.setStoreMobile(o.getStoreType() == StoreType.SHOP ? o.getShop().getMobile() : o.getRestaurant().getMobile());
        d.setStatus(o.getStatus());
        d.setStatusReason(o.getStatusReason());
        d.setCustomerName(o.getCustomer().getFullName());
        d.setContactName(o.getContactName());
        d.setContactMobile(o.getContactMobile());
        d.setDeliveryAddress(o.getDeliveryAddress());
        d.setDeliveryLatitude(o.getDeliveryLatitude());
        d.setDeliveryLongitude(o.getDeliveryLongitude());
        d.setInstructions(o.getInstructions());
        for (StoreOrderItem i : o.getItems()) {
            Line l = new Line();
            l.setItemUuid(i.getItemUuid());
            l.setItemName(i.getItemName());
            l.setImageUrl(i.getImageUrl());
            l.setUnitPrice(i.getUnitPrice());
            l.setQuantity(i.getQuantity());
            l.setLineTotal(i.getLineTotal());
            d.getLines().add(l);
        }
        d.setSubtotal(o.getSubtotal());
        d.setDeliveryFee(o.getDeliveryFee());
        d.setDiscountAmount(o.getDiscountAmount());
        d.setTotalAmount(o.getTotalAmount());
        d.setCouponCode(o.getCouponCode());
        d.setPaymentMethod(o.getPaymentMethod());
        d.setPaymentPaid(o.getPaymentPaid());
        d.setEstimatedMinutes(o.getEstimatedMinutes());
        d.setDistanceKm(o.getDistanceKm());
        d.setCreatedAt(o.getCreatedAt());
        for (StoreOrderStatusLog l : o.getLogs()) {
            TimelineEntry t = new TimelineEntry();
            t.setStatus(l.getStatus());
            t.setNote(l.getNote());
            t.setAt(l.getCreatedAt());
            d.getTimeline().add(t);
        }
        d.setCancellable(o.getStatus() == StoreOrderStatus.PLACED);
        return d;
    }

    private static String logoOf(StoreOrder o) {
        return o.getStoreType() == StoreType.SHOP ? o.getShop().getLogoUrl() : o.getRestaurant().getLogoUrl();
    }

    // ================================================================ helpers

    private StoreOrder ownOrder(String uuid, SystemUser customer) {
        return orderRepository.findByUuidAndDeletedFalse(uuid)
                .filter(o -> o.getCustomer().getRecordId().equals(customer.getRecordId()))
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private static void requireCustomer(SystemUser user) {
        if (user == null || user.getUserType() != UserTypeEnum.CUSTOMER) {
            throw new ForbiddenException("access.denied");
        }
    }

    private LocalTime now() {
        ZoneId zone = (timeZone == null || timeZone.isBlank()) ? ZoneId.systemDefault() : ZoneId.of(timeZone);
        return LocalTime.now(zone).withNano(0);
    }

    private static boolean isOpenNow(Boolean accepting, LocalTime open, LocalTime close, LocalTime now) {
        if (Boolean.FALSE.equals(accepting)) {
            return false;
        }
        if (open == null || close == null) {
            return true;
        }
        if (!open.isAfter(close)) {
            return !now.isBefore(open) && now.isBefore(close);
        }
        return !now.isBefore(open) || now.isBefore(close);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
