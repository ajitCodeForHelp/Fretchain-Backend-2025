package com.bt.fretchain.primary.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Getter
@Setter
public class Banner extends BaseEntity {

    @Column(name = "title_en", length = 200)
    private String titleEn;

    @Column(name = "title_fr", length = 200)
    private String titleFr;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description_fr", length = 500)
    private String descriptionFr;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "redirect_url", length = 1000)
    private String redirectUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
}