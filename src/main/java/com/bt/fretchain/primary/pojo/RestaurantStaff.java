package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Links a login (SystemUser of type RESTAURANT_USER) to a restaurant and a role. */
@Getter
@Setter
@Entity
@Table(
    name = "restaurant_staff",
    uniqueConstraints = @UniqueConstraint(name = "uk_restaurant_staff_user", columnNames = "user_id"),
    indexes = @Index(name = "idx_restaurant_staff_restaurant", columnList = "restaurant_id")
)
public class RestaurantStaff extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantMaster restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SystemUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RestaurantRole role;

    /** The restaurant owner: has every activity and cannot be deactivated by staff. */
    @Column(name = "owner", nullable = false)
    private boolean owner = false;
}
