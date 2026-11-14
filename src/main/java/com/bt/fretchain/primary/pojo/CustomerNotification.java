package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A notification sent to a customer (also kept as an in-app feed). */
@Getter
@Setter
@Entity
@Table(name = "customer_notifications", indexes = @Index(name = "idx_cust_notif_customer", columnList = "customer_id"))
public class CustomerNotification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private SystemUser customer;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    /** SHOP | FOOD | FREIGHT */
    @Column(name = "order_type", length = 20)
    private String orderType;

    @Column(name = "order_uuid", length = 64)
    private String orderUuid;

    @Column(name = "pushed")
    private Boolean pushed = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
