package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Platform-level (admin managed) category used to bifurcate shops,
 * e.g. Grocery, Pharmacy, Electronics, Bakery, Meat & Fish.
 *
 * A shop is assigned one or more of these at creation time
 * (see {@link ShopMaster#getGlobalCategories()}). The customer home screen shows
 * them as chips and the nearby-store list can be filtered by them.
 */
@Getter
@Setter
@Entity
@Table(
    name = "shop_global_category",
    indexes = {
        @Index(name = "idx_shop_gcat_name", columnList = "name_en")
    }
)
public class ShopGlobalCategory extends BaseEntity {

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_fr", length = 150)
    private String nameFr;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "description_fr", columnDefinition = "TEXT")
    private String descriptionFr;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
