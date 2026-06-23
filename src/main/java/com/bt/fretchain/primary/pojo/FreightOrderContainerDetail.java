package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.ContainerLoadType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "freight_order_container_details")
@Getter
@Setter
public class FreightOrderContainerDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id", nullable = false, unique = true)
    private FreightOrder freightOrder;
    @Enumerated(EnumType.STRING)
    @Column(name = "load_type", length = 50)
    private ContainerLoadType loadType; // EMPTY, LADEN

    @Column(name = "dangerous_goods")
    private Boolean dangerousGoods = false;

    @Column(name = "nature_of_goods", length = 500)
    private String natureOfGoods;

    @Column(name = "shipping_line", length = 255)
    private String shippingLine;

    @Column(name = "shipping_line_email", length = 255)
    private String shippingLineEmail;

    @Column(name = "cargo_weight_kg")
    private Double cargoWeightKg;

    @Column(name = "insurance")
    private Boolean insurance = false;

    @Column(name = "cargo_value", precision = 14, scale = 2)
    private BigDecimal cargoValue;

    @Column(name = "return_empty_container")
    private Boolean returnEmptyContainer = false;
}