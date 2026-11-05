package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.PlatformActivity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Role for platform admin staff (e.g. Accountant, Order Manager), created by the admin
 * with the admin-panel menus / actions it may use.
 */
@Getter
@Setter
@Entity
@Table(name = "platform_role")
public class PlatformRole extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "platform_role_activity", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "activity", length = 50, nullable = false)
    private Set<PlatformActivity> activities = new LinkedHashSet<>();
}
