package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "coupon_usages")
@Getter
@Setter
public class CouponUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private SystemUser customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id")
    private FreightOrder freightOrder;

    /** Set when the coupon was used on a shop / food order. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id")
    private StoreOrder storeOrder;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;
}