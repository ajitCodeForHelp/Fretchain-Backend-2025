package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.FoodType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
    name = "restaurant_item",
    indexes = {
        @Index(name = "idx_restaurant_item_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_restaurant_item_category", columnList = "category_id"),
        @Index(name = "idx_restaurant_item_subcategory", columnList = "subcategory_id"),
        @Index(name = "idx_restaurant_item_name", columnList = "name_en")
    }
)
/** A dish on a restaurant menu. Cooked to order, so no stock / SKU tracking. */
public class RestaurantItem extends BaseEntity {

    @Column(name = "item_code", length = 50)
    private String itemCode;

    @Column(name = "name_en", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "name_fr", length = 200)
    private String nameFr;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "description_fr", columnDefinition = "TEXT")
    private String descriptionFr;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    /** VEG / NON_VEG / EGG. */
    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", length = 20)
    private FoodType foodType;

    /** Serving description, e.g. "Serves 2", "Half", "8 pcs". */
    @Column(name = "serving_size", length = 100)
    private String servingSize;

    /** Preparation time for this dish in minutes (optional). */
    @Column(name = "preparation_minutes")
    private Integer preparationMinutes;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Comma separated search keywords / synonyms, e.g. "paneer,cottage cheese".
     * Matched by customer search together with name and category names.
     */
    @Column(name = "search_tags", length = 500)
    private String searchTags;

    /** Owner can switch a dish off when it is sold out for the day. */
    @Column(nullable = false)
    private Boolean available = true;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantMaster restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private RestaurantCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private RestaurantSubCategory subCategory;
}