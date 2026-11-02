package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(
    name = "shop_master",
    indexes = {
        @Index(name = "idx_shop_owner", columnList = "owner_id"),
        @Index(name = "idx_shop_mobile", columnList = "mobile"),
        @Index(name = "idx_shop_status", columnList = "status")
    }
)
public class ShopMaster extends BaseEntity {

    @Column(name = "shop_code", unique = true, length = 50)
    private String shopCode;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_fr", length = 150)
    private String nameFr;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "description_fr", columnDefinition = "TEXT")
    private String descriptionFr;

    @Column(length = 20)
    private String countryCode;

    @Column(length = 20)
    private String mobile;

    @Column(length = 150)
    private String email;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(
        name = "commission_percentage",
        precision = 5,
        scale = 2
    )
    private BigDecimal commissionPercentage = BigDecimal.ZERO;

    // PENDING, APPROVED, REJECTED, SUSPENDED
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private SystemUser owner;
}