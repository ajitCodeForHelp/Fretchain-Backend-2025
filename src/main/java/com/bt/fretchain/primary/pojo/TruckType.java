package com.bt.fretchain.primary.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "truck_types")
@Getter
@Setter
public class TruckType extends BaseEntity {

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "license_required")
    private Boolean licenseRequired = true;
}