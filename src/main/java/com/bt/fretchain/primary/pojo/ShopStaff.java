package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Links a login (SystemUser of type SHOP_USER) to a shop and a role. */
@Getter
@Setter
@Entity
@Table(
    name = "shop_staff",
    uniqueConstraints = @UniqueConstraint(name = "uk_shop_staff_user", columnNames = "user_id"),
    indexes = @Index(name = "idx_shop_staff_shop", columnList = "shop_id")
)
public class ShopStaff extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopMaster shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SystemUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private ShopRole role;

    /** The shop owner: has every activity and cannot be deactivated by staff. */
    @Column(name = "owner", nullable = false)
    private boolean owner = false;
}
