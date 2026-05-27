package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "truck_order_additional_services")
@Getter
@Setter
public class TruckOrderAdditionalService extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_order_id")
    private TruckOrder truckOrder;

    @Column(name = "packing")
    private Boolean packing = false;

    @Column(name = "trolley")
    private Boolean trolley = false;

    @Column(name = "wooden_pallet")
    private Boolean woodenPallet = false;

    @Column(name = "wooden_pallet_count")
    private Integer woodenPalletCount = 0;

    @Column(name = "carton_box")
    private Boolean cartonBox = false;

    @Column(name = "carton_box_count")
    private Integer cartonBoxCount = 0;

    @Column(name = "helper_count")
    private Integer helperCount = 0;
}