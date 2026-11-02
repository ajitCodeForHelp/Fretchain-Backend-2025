package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.FreightOrderType;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.PickupType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "freight_orders")
@Getter
@Setter
public class FreightOrder extends BaseEntity {

    @Column(name = "order_number", unique = true, length = 100)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 50)
    private FreightOrderType orderType; // CONTAINER, BREAK_BULK

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 50)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_type", length = 30)
    private PickupType pickupType = PickupType.NOW;

    @Column(name = "pickup_date_time")
    private LocalDateTime pickupDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private SystemUser customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private SystemUser vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_category_id")
    private VehicleCategory vehicleCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_address_id")
    private FreightOrderAddress pickupAddress;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "freight_order_drop_addresses",
            joinColumns = @JoinColumn(name = "freight_order_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id")
    )
    private List<FreightOrderAddress> dropAddresses = new ArrayList<>();

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    @Column(name = "instruction_for_driver", columnDefinition = "TEXT")
    private String instructionForDriver;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "coupon_discount_amount", precision = 12, scale = 2)
    private BigDecimal couponDiscountAmount;
}