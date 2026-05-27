package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trucks")
@Getter
@Setter
public class Truck extends BaseEntity {

    @Column(name = "truck_number", nullable = false, unique = true, length = 50)
    private String truckNumber;

    @Column(name = "rc_number", length = 100)
    private String rcNumber;

    @Column(name = "fc_number", length = 100)
    private String fcNumber;

    @Column(name = "chassis_number", length = 150)
    private String chassisNumber;

    @Column(name = "max_weight_kg")
    private Long maxWeightKg;

    @Column(name = "verified")
    private Boolean verified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_category_id")
    private TruckCategory truckCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_user_id")
    private SystemUser vendor;

    @OneToOne(mappedBy = "truck")
    private Driver driver;
}