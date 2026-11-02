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
    name = "shop_item",
    indexes = {
        @Index(name = "idx_shop_item_shop", columnList = "shop_id"),
        @Index(name = "idx_shop_item_category", columnList = "category_id"),
        @Index(name = "idx_shop_item_subcategory", columnList = "subcategory_id"),
        @Index(name = "idx_shop_item_sku", columnList = "sku"),
        @Index(name = "idx_shop_item_name", columnList = "name_en")
    }
)
public class ShopItem extends BaseEntity {

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

    @Column(length = 100)
    private String sku;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(length = 50)
    private String unit;

    @Column(length = 150)
    private String brand;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** VEG / NON_VEG / EGG - mainly for restaurant items, null for others. */
    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", length = 20)
    private FoodType foodType;

    /**
     * Comma separated search keywords / synonyms, e.g. "atta,flour,wheat".
     * Matched by customer search together with name, brand and category names.
     */
    @Column(name = "search_tags", length = 500)
    private String searchTags;

    /** Restaurant: preparation time for this dish (optional). */
    @Column(name = "preparation_minutes")
    private Integer preparationMinutes;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopMaster shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ShopCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private ShopSubCategory subCategory;
}