package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private SystemUser customer;

    @Column(name = "address_type", length = 50)
    private String addressType; // HOME, OFFICE, WAREHOUSE, PORT, OTHER

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

    @Column(name = "default_address")
    private Boolean defaultAddress = false;
}