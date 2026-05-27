package com.bt.fretchain.primary.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address extends BaseEntity {

    @Column(name = "address_name", length = 100)
    private String addressName;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    @Column(name = "landmark", length = 255)
    private String landmark;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}