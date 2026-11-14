package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import com.bt.fretchain.primary.pojo.enums.StorePaymentMethod;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A customer order placed with a shop or a restaurant (food delivery). */
@Getter
@Setter
@Entity
@Table(name = "store_orders", indexes = {
        @Index(name = "idx_store_order_shop", columnList = "shop_id"),
        @Index(name = "idx_store_order_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_store_order_customer", columnList = "customer_id"),
        @Index(name = "idx_store_order_status", columnList = "status")
})
public class StoreOrder extends BaseEntity {

    @Column(name = "order_number", length = 30)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_type", nullable = false, length = 15)
    private StoreType storeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private ShopMaster shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private RestaurantMaster restaurant;

    /** Name at the time of the order (kept even if the store is renamed). */
    @Column(name = "store_name", length = 150)
    private String storeName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private SystemUser customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private StoreOrderStatus status = StoreOrderStatus.PLACED;

    /** Reason when the store rejects or the customer cancels. */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_mobile", length = 30)
    private String contactMobile;

    @Column(name = "instructions", length = 500)
    private String instructions;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 12, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 25)
    private StorePaymentMethod paymentMethod = StorePaymentMethod.CASH_ON_DELIVERY;

    /** PENDING until delivered (cash on delivery), then PAID. */
    @Column(name = "payment_paid")
    private Boolean paymentPaid = false;

    /** Driver who delivers the order (assigned by the admin, or automatically in AUTO dispatch mode). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "driver_assigned_at")
    private java.time.LocalDateTime driverAssignedAt;

    /** Last step the driver reported that the status does not show: ARRIVED_STORE or ARRIVED_CUSTOMER. */
    @Column(name = "driver_step", length = 30)
    private String driverStep;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, recordId ASC")
    private List<StoreOrderStatusLog> logs = new ArrayList<>();

    public String storeUuid() {
        return storeType == StoreType.SHOP ? shop.getUuid() : restaurant.getUuid();
    }
}
