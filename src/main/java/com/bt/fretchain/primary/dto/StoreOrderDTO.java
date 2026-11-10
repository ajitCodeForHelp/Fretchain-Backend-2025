package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import com.bt.fretchain.primary.pojo.enums.StorePaymentMethod;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Shop / food orders: customer checkout, order tracking and the store's order screen. */
public class StoreOrderDTO {

    // =====================================================================
    // Customer requests
    // =====================================================================

    @Getter
    @Setter
    public static class CartLine {
        @NotBlank
        private String itemUuid;

        @NotNull
        @Min(1)
        @Max(50)
        private Integer quantity;
    }

    /** Price a cart (no login needed to browse; checkout and coupons need a login). */
    @Getter
    @Setter
    public static class QuoteRequest {
        @NotNull
        private StoreType storeType;

        @NotBlank
        private String storeUuid;

        @NotEmpty
        @Valid
        private List<CartLine> items = new ArrayList<>();

        /** Optional coupon code typed by the customer. */
        @Size(max = 50)
        private String couponCode;

        /** Delivery location: used for the delivery area check and the delivery time. */
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private Double latitude;

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private Double longitude;
    }

    @Getter
    @Setter
    public static class PlaceRequest extends QuoteRequest {
        @NotBlank
        @Size(max = 500)
        private String deliveryAddress;

        @NotBlank
        @Size(max = 150)
        private String contactName;

        @NotBlank
        @Size(max = 30)
        private String contactMobile;

        @Size(max = 500)
        private String instructions;

        private StorePaymentMethod paymentMethod = StorePaymentMethod.CASH_ON_DELIVERY;
    }

    @Getter
    @Setter
    public static class ListRequest {
        /** true = orders in progress, false = past orders, null = all. */
        private Boolean active;

        @Min(0)
        private Integer pageNumber = 0;

        @Min(1)
        @Max(50)
        private Integer pageSize = 10;
    }

    @Getter
    @Setter
    public static class CancelRequest {
        @Size(max = 500)
        private String reason;
    }

    // =====================================================================
    // Quote response
    // =====================================================================

    /**
     * A problem with the cart. code is one of:
     * STORE_UNAVAILABLE, STORE_CLOSED, OUT_OF_AREA, MIN_ORDER (value = minimum),
     * ITEM_NOT_FOUND, ITEM_UNAVAILABLE, ITEM_STOCK (value = left in stock).
     */
    @Getter
    @Setter
    public static class Issue {
        private String code;
        private String itemUuid;
        private String itemName;
        private BigDecimal value;

        public Issue() {
        }

        public Issue(String code, String itemUuid, String itemName, BigDecimal value) {
            this.code = code;
            this.itemUuid = itemUuid;
            this.itemName = itemName;
            this.value = value;
        }
    }

    @Getter
    @Setter
    public static class Line {
        private String itemUuid;
        private String itemName;
        private String imageUrl;
        private BigDecimal unitPrice;
        private BigDecimal originalPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
    }

    @Getter
    @Setter
    public static class Quote {
        private StoreType storeType;
        private String storeUuid;
        private String storeName;
        private List<Line> lines = new ArrayList<>();
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal deliveryFee = BigDecimal.ZERO;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal minOrderAmount;
        private Double distanceKm;
        private Integer estimatedMinutes;
        /** Coupon accepted for this cart (null when none or invalid). */
        private String couponCode;
        /** COUPON_NOT_FOUND, COUPON_INACTIVE, COUPON_EXPIRED, COUPON_NOT_STARTED, COUPON_MIN_ORDER (value), COUPON_USED_UP, COUPON_LIMIT_REACHED. */
        private Issue couponIssue;
        private List<Issue> issues = new ArrayList<>();
        /** False while there are cart issues: the customer must fix the cart first. */
        private boolean canPlace;
    }

    @Getter
    @Setter
    public static class CouponInfo {
        private String code;
        private String title;
        private String description;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal maxDiscountAmount;
        private BigDecimal minimumOrderAmount;
        private LocalDateTime validUntil;
    }

    // =====================================================================
    // Order responses
    // =====================================================================

    @Getter
    @Setter
    public static class TimelineEntry {
        private StoreOrderStatus status;
        private String note;
        private LocalDateTime at;
    }

    /** Row of an order list (customer or store). */
    @Getter
    @Setter
    public static class Card {
        private String uuid;
        private String orderNumber;
        private StoreType storeType;
        private String storeName;
        private String storeLogoUrl;
        private StoreOrderStatus status;
        private BigDecimal totalAmount;
        private Integer itemCount;
        /** "2 x Pizza, 1 x Cola" */
        private String itemsSummary;
        private String customerName;
        private String contactMobile;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    public static class Detail {
        private String uuid;
        private String orderNumber;
        private StoreType storeType;
        private String storeUuid;
        private String storeName;
        private String storeLogoUrl;
        private String storeMobile;
        private StoreOrderStatus status;
        private String statusReason;

        private String customerName;
        private String contactName;
        private String contactMobile;
        private String deliveryAddress;
        private Double deliveryLatitude;
        private Double deliveryLongitude;
        private String instructions;

        private List<Line> lines = new ArrayList<>();
        private BigDecimal subtotal;
        private BigDecimal deliveryFee;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private String couponCode;
        private StorePaymentMethod paymentMethod;
        private Boolean paymentPaid;
        private Integer estimatedMinutes;
        private Double distanceKm;
        private LocalDateTime createdAt;
        private List<TimelineEntry> timeline = new ArrayList<>();

        /** Customer: true while the order can still be cancelled. */
        private Boolean cancellable;
        /** Store: statuses the logged-in staff member may move the order to (empty without ORDER_STATUS). */
        private List<StoreOrderStatus> nextStatuses = new ArrayList<>();
    }

    // =====================================================================
    // Store (partner) side
    // =====================================================================

    @Getter
    @Setter
    public static class PartnerListRequest {
        /** One status, or null for all. */
        private StoreOrderStatus status;

        /** Several statuses (a tab such as "In progress"). Used when no single status is given. */
        private List<StoreOrderStatus> statuses;

        /** true = orders in progress. Used when no single status is given. */
        private Boolean active;

        /** Order number, customer name or mobile. */
        @Size(max = 100)
        private String search;

        @Min(0)
        private Integer pageNumber = 0;

        @Min(1)
        @Max(100)
        private Integer pageSize = 20;
    }

    /** Admin: orders of all shops and restaurants, optionally of one type or one store. */
    @Getter
    @Setter
    public static class AdminListRequest extends PartnerListRequest {
        private StoreType storeType;
        private String storeUuid;
    }

    @Getter
    @Setter
    public static class StatusUpdate {
        @NotNull
        private StoreOrderStatus status;

        /** Required when rejecting. */
        @Size(max = 500)
        private String reason;
    }

    @Getter
    @Setter
    public static class Summary {
        /** status -> number of orders */
        private java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        private long activeOrders;
        private long ordersToday;
        private BigDecimal salesToday = BigDecimal.ZERO;
    }
}
