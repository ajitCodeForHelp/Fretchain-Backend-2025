package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A role defined by a shop (e.g. Manager, Catalogue Editor) with the
 * activities it may perform. The owner role is created automatically,
 * always has every activity and cannot be edited.
 */
@Getter
@Setter
@Entity
@Table(
    name = "shop_role",
    indexes = @Index(name = "idx_shop_role_shop", columnList = "shop_id")
)
public class ShopRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopMaster shop;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "owner_role", nullable = false)
    private boolean ownerRole = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "shop_role_activity", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "activity", length = 50, nullable = false)
    private Set<ShopActivity> activities = new LinkedHashSet<>();
}
