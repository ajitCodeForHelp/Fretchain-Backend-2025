package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "freight_order_bulk_material_details")
@Getter
@Setter
public class FreightOrderBulkMaterialDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id", nullable = false, unique = true)
    private FreightOrder freightOrder;

    @Column(name = "material_type", length = 100)
    private String materialType; // Sand, Gravel, Cement, Minerals, Other

    @Column(name = "other_material_type", length = 255)
    private String otherMaterialType;

    @Column(name = "quantity_unit", length = 50)
    private String quantityUnit; // TONS, CUBIC_METERS

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "estimated_weight_kg")
    private Double estimatedWeightKg;

    @Column(name = "loading_site", length = 500)
    private String loadingSite;

    @Column(name = "site_accessibility", length = 100)
    private String siteAccessibility; // GOOD_ROAD, OFF_ROAD, MUDDY, MOUNTAIN

    @Column(name = "delivery_site", length = 500)
    private String deliverySite;

    @Column(name = "delivery_time_window", length = 100)
    private String deliveryTimeWindow;

    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    private String deliveryInstructions;

    @Column(name = "number_of_trucks_needed")
    private Integer numberOfTrucksNeeded;

    @Column(name = "estimated_number_of_trips")
    private Integer estimatedNumberOfTrips;
}