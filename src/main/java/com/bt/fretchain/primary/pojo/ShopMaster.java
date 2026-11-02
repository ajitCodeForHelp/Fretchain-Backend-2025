package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.BusinessType;
import com.bt.fretchain.primary.pojo.enums.FoodType;
import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A store in the marketplace: either a SHOP or a RESTAURANT ({@link #businessType}).
 *
 * Hierarchy:
 *   ShopGlobalCategory (platform, many-to-many)  <-- ShopMaster
 *   ShopMaster -> ShopCategory -> ShopSubCategory -> ShopItem   (store's own catalogue / menu)
 */
@Getter
@Setter
@Entity
@Table(
    name = "shop_master",
    indexes = {
        @Index(name = "idx_shop_owner", columnList = "owner_id"),
        @Index(name = "idx_shop_mobile", columnList = "mobile"),
        @Index(name = "idx_shop_status", columnList = "status"),
        // Used by the nearby query (bounding-box pre-filter before Haversine)
        @Index(name = "idx_shop_type_geo", columnList = "business_type, latitude, longitude")
    }
)
public class ShopMaster extends BaseEntity {

    @Column(name = "shop_code", unique = true, length = 50)
    private String shopCode;

    /** SHOP or RESTAURANT. Existing rows default to SHOP. */
    @Enumerated(EnumType.STRING)
    @Column(
        name = "business_type",
        nullable = false,
        length = 20,
        columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'SHOP'"
    )
    private BusinessType businessType = BusinessType.SHOP;

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

    // ---------------- Location ----------------

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

    /**
     * How far this store delivers. A store is shown to a customer only when the
     * customer is within this radius. Null = platform default radius.
     */
    @Column(name = "delivery_radius_km", precision = 6, scale = 2)
    private BigDecimal deliveryRadiusKm;

    // ---------------- Timing / availability ----------------

    @Column(name = "opening_time")
    private LocalTime openingTime;

    /** May be earlier than openingTime for stores open past midnight. */
    @Column(name = "closing_time")
    private LocalTime closingTime;

    /**
     * Manual "pause orders" switch for the store owner (busy kitchen, out of stock...).
     * Null is treated as true.
     */
    @Column(name = "accepting_orders")
    private Boolean acceptingOrders = true;

    /** Average order preparation / packing time in minutes. */
    @Column(name = "avg_preparation_minutes")
    private Integer avgPreparationMinutes;

    // ---------------- Commercials ----------------

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "delivery_fee", precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "commission_percentage", precision = 5, scale = 2)
    private BigDecimal commissionPercentage = BigDecimal.ZERO;

    // ---------------- Restaurant specific (nullable for shops) ----------------

    /** VEG = pure veg restaurant, BOTH = veg + non-veg. */
    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", length = 20)
    private FoodType foodType;

    @Column(name = "cost_for_two", precision = 12, scale = 2)
    private BigDecimal costForTwo;

    // ---------------- Ratings (denormalised, updated by review module) ----------------

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    // ---------------- Moderation ----------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShopStatus status = ShopStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private SystemUser owner;

    /**
     * Global categories this store belongs to (e.g. Grocery + Bakery, or Pizza + Italian).
     * Customers filter nearby stores by these.
     */
    @BatchSize(size = 50)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "shop_global_category_map",
        joinColumns = @JoinColumn(name = "shop_id"),
        inverseJoinColumns = @JoinColumn(name = "global_category_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_shop_gcat_map",
            columnNames = {"shop_id", "global_category_id"}
        ),
        indexes = @Index(name = "idx_shop_gcat_map_gcat", columnList = "global_category_id")
    )
    private Set<ShopGlobalCategory> globalCategories = new LinkedHashSet<>();
}
