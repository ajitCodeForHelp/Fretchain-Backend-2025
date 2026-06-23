package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "freight_order_breakbulk_details")
@Getter
@Setter
public class FreightOrderBreakBulkDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id", nullable = false, unique = true)
    private FreightOrder freightOrder;

    @Column(name = "cargo_type", length = 100)
    private String cargoType;

    @Column(name = "other_cargo_type", length = 255)
    private String otherCargoType;

    @Column(name = "cargo_details", columnDefinition = "TEXT")
    private String cargoDetails;

    @Column(name = "nature_of_goods", length = 500)
    private String natureOfGoods;

    @Column(name = "total_weight_kg")
    private Double totalWeightKg;

    @Column(name = "total_volume_cbm")
    private Double totalVolumeCbm;

    @Column(name = "number_of_pieces")
    private Integer numberOfPieces;

    @Column(name = "dimensions_per_piece", length = 255)
    private String dimensionsPerPiece;

    @Column(name = "fragile")
    private Boolean fragile = false;

    @Column(name = "handling_required")
    private Boolean handlingRequired = false;

    @Column(name = "safety_required")
    private Boolean safetyRequired = false;

    @Column(name = "hazardous_material")
    private Boolean hazardousMaterial = false;

    @Column(name = "temperature_sensitive")
    private Boolean temperatureSensitive = false;

    @Column(name = "insurance_required")
    private Boolean insuranceRequired = false;

    @Column(name = "forklift_needed")
    private Boolean forkliftNeeded = false;

    @Column(name = "crane_needed")
    private Boolean craneNeeded = false;

    @Column(name = "trolley_needed")
    private Boolean trolleyNeeded = false;

    @Column(name = "manual_labor_required")
    private Boolean manualLaborRequired = false;
}