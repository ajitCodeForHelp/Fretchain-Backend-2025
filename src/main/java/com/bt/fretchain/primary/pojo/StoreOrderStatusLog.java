package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Status history shown as the tracking timeline. */
@Getter
@Setter
@Entity
@Table(name = "store_order_status_logs")
public class StoreOrderStatusLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private StoreOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private StoreOrderStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "changed_by", length = 150)
    private String changedBy;
}
