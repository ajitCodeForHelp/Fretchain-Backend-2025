package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "truck_order_amount_split")
@Getter
@Setter
public class TruckOrderAmountSplit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_order_id")
    private TruckOrder truckOrder;

    @Column(name = "driver_earnings", precision = 12, scale = 2)
    private BigDecimal driverEarnings;

    @Column(name = "admin_commission", precision = 12, scale = 2)
    private BigDecimal adminCommission;

    @Column(name = "insurance_charges", precision = 12, scale = 2)
    private BigDecimal insuranceCharges;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;
}