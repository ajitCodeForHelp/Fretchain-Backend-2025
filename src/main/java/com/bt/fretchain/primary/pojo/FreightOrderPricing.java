package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "freight_order_pricing")
@Getter
@Setter
public class FreightOrderPricing extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id", nullable = false, unique = true)
    private FreightOrder freightOrder;

    @Column(name = "base_charges", precision = 14, scale = 2)
    private BigDecimal baseCharges = BigDecimal.ZERO;

    @Column(name = "base_fare", precision = 14, scale = 2)
    private BigDecimal baseFare = BigDecimal.ZERO;

    @Column(name = "extra_km_fare", precision = 14, scale = 2)
    private BigDecimal extraKmFare = BigDecimal.ZERO;

    @Column(name = "insurance_charges", precision = 14, scale = 2)
    private BigDecimal insuranceCharges = BigDecimal.ZERO;

    @Column(name = "helper_charges", precision = 14, scale = 2)
    private BigDecimal helperCharges = BigDecimal.ZERO;

    @Column(name = "waiting_charges", precision = 14, scale = 2)
    private BigDecimal waitingCharges = BigDecimal.ZERO;

    @Column(name = "multi_drop_charges", precision = 14, scale = 2)
    private BigDecimal multiDropCharges = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "sub_total", precision = 14, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(name = "driver_earnings", precision = 14, scale = 2)
    private BigDecimal driverEarnings = BigDecimal.ZERO;

    @Column(name = "vendor_earnings", precision = 14, scale = 2)
    private BigDecimal vendorEarnings = BigDecimal.ZERO;

    @Column(name = "admin_commission", precision = 14, scale = 2)
    private BigDecimal adminCommission = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
}