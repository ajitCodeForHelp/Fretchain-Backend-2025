package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A vendor / fleet owner: the company that owns vehicles and employs drivers.
 * Its login is a SystemUser of type VENDOR (same uuid is used as "vendorUuid"
 * on vehicles and drivers, because those reference the login user).
 */
@Getter
@Setter
@Entity
@Table(name = "vendor_master")
public class VendorMaster extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private SystemUser user;

    @Column(name = "vendor_code", length = 50)
    private String vendorCode;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "country_code", length = 20)
    private String countryCode;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "commission_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal commissionPercentage;
}
