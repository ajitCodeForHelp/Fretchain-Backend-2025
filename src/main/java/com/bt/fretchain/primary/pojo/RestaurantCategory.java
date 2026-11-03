package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "restaurant_category",
    indexes = {
        @Index(name = "idx_restaurant_category_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_restaurant_category_name", columnList = "name_en"),
        @Index(name = "idx_restaurant_category_gcat", columnList = "global_category_id")
    }
)
public class RestaurantCategory extends BaseEntity {

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
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantMaster restaurant;

    /**
     * Optional link to one of the restaurant's global categories
     * (e.g. menu section "Veg Pizzas" -> cuisine "Pizza").
     * Must be one of {@link RestaurantMaster#getGlobalCategories()}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "global_category_id")
    private RestaurantGlobalCategory globalCategory;
}