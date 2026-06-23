package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "freight_order_container_items")
@Getter
@Setter
public class FreightOrderContainerItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_detail_id", nullable = false)
    private FreightOrderContainerDetail containerDetail;

    @Column(name = "container_number", length = 100)
    private String containerNumber;

    @Column(name = "seal_number", length = 100)
    private String sealNumber;
}