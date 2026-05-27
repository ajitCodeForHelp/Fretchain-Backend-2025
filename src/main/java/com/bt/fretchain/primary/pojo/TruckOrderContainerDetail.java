package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "truck_order_container_details")
@Getter
@Setter
public class TruckOrderContainerDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_order_id")
    private TruckOrder truckOrder;

    @Column(name = "container_number", length = 100)
    private String containerNumber;

    @Column(name = "seal_number", length = 100)
    private String sealNumber;

    @Column(name = "shipping_line", length = 255)
    private String shippingLine;

    @Column(name = "return_empty_container")
    private Boolean returnEmptyContainer = false;
}