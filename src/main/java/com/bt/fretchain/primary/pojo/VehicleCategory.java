package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_categories")
@Getter
@Setter
public class VehicleCategory extends BaseEntity {

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_fr", nullable = false, length = 150)
    private String nameFr;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @Column(name = "length_cm")
    private Long lengthCm;

    @Column(name = "width_cm")
    private Long widthCm;

    @Column(name = "height_cm")
    private Long heightCm;

    @Column(name = "max_weight_kg")
    private Long maxWeightKg;

    @Column(name = "base_charges", precision = 12, scale = 2)
    private BigDecimal baseCharges;

    @Column(name = "base_fare", precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "base_km")
    private Double baseKm;

    @Column(name = "per_km_fare", precision = 12, scale = 2)
    private BigDecimal perKmFare;

    @Column(name = "multi_drop_charges", precision = 12, scale = 2)
    private BigDecimal multiDropCharges;

    @Column(name = "special_help_available")
    private Boolean specialHelpAvailable = false;

    @Column(name = "cash_services")
    private Boolean cashServices = false;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * Services this category can serve. Empty (older rows) means freight only.
     * e.g. a bike: SHOP_DELIVERY + FOOD_DELIVERY; a truck: FREIGHT.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "vehicle_category_service", joinColumns = @JoinColumn(name = "vehicle_category_id"))
    @Column(name = "service", length = 30, nullable = false)
    private java.util.Set<com.bt.fretchain.primary.pojo.enums.DeliveryService> services = new java.util.LinkedHashSet<>();

    public boolean supports(com.bt.fretchain.primary.pojo.enums.DeliveryService service) {
        return services == null || services.isEmpty()
                ? service == com.bt.fretchain.primary.pojo.enums.DeliveryService.FREIGHT
                : services.contains(service);
    }
}
