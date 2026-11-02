package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.CouponDiscountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
public class Coupon extends BaseEntity {

    @Column(name = "coupon_code", nullable = false, unique = true, length = 50)
    private String couponCode;

    @Column(name = "title_en", length = 200)
    private String titleEn;

    @Column(name = "title_fr", length = 200)
    private String titleFr;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description_fr", length = 500)
    private String descriptionFr;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private CouponDiscountType discountType;

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "minimum_order_amount", precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "per_customer_limit")
    private Integer perCustomerLimit = 1;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
}