package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "truck_orders")
@Getter
@Setter
public class TruckOrder extends BaseEntity {

    @Column(name = "order_number", unique = true, length = 100)
    private String orderNumber;

    @Column(name = "order_date_time")
    private LocalDateTime orderDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 50)
    private OrderStatus orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_address_id")
    private Address pickupAddress;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "truck_order_drop_addresses",
            joinColumns = @JoinColumn(name = "truck_order_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id")
    )
    private List<Address> dropAddresses = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_category_id")
    private TruckCategory truckCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "estimated_time")
    private Double estimatedTime;

    @Column(name = "cargo_description", length = 1000)
    private String cargoDescription;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "dimensions", length = 255)
    private String dimensions;

    @Column(name = "insurance")
    private Boolean insurance = false;

    @Column(name = "cargo_value", precision = 14, scale = 2)
    private BigDecimal cargoValue;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;
}