package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class Vehicle extends BaseEntity {

    @Column(name = "vehicle_number", nullable = false, unique = true, length = 50)
    private String vehicleNumber;

    @Column(name = "rc_number", length = 100)
    private String rcNumber;

    @Column(name = "fitness_certificate_number", length = 100)
    private String fitnessCertificateNumber;

    @Column(name = "chassis_number", length = 150)
    private String chassisNumber;

    @Column(name = "max_weight_kg")
    private Long maxWeightKg;

    @Column(name = "verified")
    private Boolean verified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_category_id")
    private VehicleCategory vehicleCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private SystemUser vendor;

    @OneToOne(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private Driver driver;
}