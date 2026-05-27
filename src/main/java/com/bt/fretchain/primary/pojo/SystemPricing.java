package com.bt.fretchain.primary.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "system_pricing")
@Getter
@Setter
public class SystemPricing extends BaseEntity {

    @Column(name = "admin_service_charge", precision = 12, scale = 2)
    private BigDecimal adminServiceCharge;

    @Column(name = "insurance_percentage", precision = 5, scale = 2)
    private BigDecimal insurancePercentage;

    @Column(name = "helper_charge", precision = 12, scale = 2)
    private BigDecimal helperCharge;

    @Column(name = "waiting_charge_per_minute", precision = 12, scale = 2)
    private BigDecimal waitingChargePerMinute;

    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    @Column(name = "cash_service_percentage", precision = 5, scale = 2)
    private BigDecimal cashServicePercentage;
}