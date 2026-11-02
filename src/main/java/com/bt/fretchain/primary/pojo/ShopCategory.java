package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "shop_category",
    indexes = {
        @Index(name = "idx_shop_category_shop", columnList = "shop_id"),
        @Index(name = "idx_shop_category_name", columnList = "name_en")
    }
)
public class ShopCategory extends BaseEntity {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopMaster shop;
}