package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** One line of a store order. Name and price are copied so later menu changes do not alter old orders. */
@Getter
@Setter
@Entity
@Table(name = "store_order_items")
public class StoreOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private StoreOrder order;

    @Column(name = "item_uuid", length = 64)
    private String itemUuid;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "line_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotal;
}
