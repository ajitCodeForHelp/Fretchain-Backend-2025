package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "freight_order_parcel_details")
@Getter
@Setter
public class FreightOrderParcelDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id", nullable = false, unique = true)
    private FreightOrder freightOrder;

    @Column(name = "package_type", length = 100)
    private String packageType;

    @Column(name = "number_of_packages")
    private Integer numberOfPackages;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "fragile")
    private Boolean fragile = false;

    @Column(name = "cod_amount", precision = 12, scale = 2)
    private BigDecimal codAmount;

    @Column(name = "package_value", precision = 12, scale = 2)
    private BigDecimal packageValue;

    @Column(name = "packaging_type", length = 100)
    private String packagingType;

    @Column(name = "express_delivery")
    private Boolean expressDelivery = false;

    @Column(name = "same_day")
    private Boolean sameDay = false;

    @Column(name = "scheduled_delivery_time")
    private LocalDateTime scheduledDeliveryTime;
}