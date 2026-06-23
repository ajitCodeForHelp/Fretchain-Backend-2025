package com.bt.fretchain.primary.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
public class VehicleType extends BaseEntity {

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "name_fr", nullable = false, length = 120)
    private String nameFr;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;
}