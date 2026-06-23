package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.AddressRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "freight_order_addresses")
@Getter
@Setter
public class FreightOrderAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id", nullable = false)
    private FreightOrder freightOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_role")
    private AddressRole addressRole;

    @Column(name = "sequence_no")
    private Integer sequenceNo; // for multi drop

    @Column(name = "address_type", length = 50)
    private String addressType;

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