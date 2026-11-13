package com.bt.fretchain.primary.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
public class PlatformSetting extends BaseEntity {

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "support_email", length = 150)
    private String supportEmail;

    @Column(name = "support_phone", length = 30)
    private String supportPhone;

    @Column(name = "support_whatsapp", length = 30)
    private String supportWhatsapp;

    @Column(name = "company_address", length = 500)
    private String companyAddress;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "currency_symbol", length = 10)
    private String currencySymbol;

    @Column(name = "platform_maintenance_mode")
    private Boolean platformMaintenanceMode = false;

    @Column(name = "privacy_policy_url", length = 1000)
    private String privacyPolicyUrl;

    @Column(name = "terms_condition_url", length = 1000)
    private String termsConditionUrl;

    @Column(name = "facebook_url", length = 1000)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 1000)
    private String instagramUrl;

    @Column(name = "linkedin_url", length = 1000)
    private String linkedinUrl;

    @Column(name = "youtube_url", length = 1000)
    private String youtubeUrl;

    @Column(name = "twitter_url", length = 1000)
    private String twitterUrl;


    /** How shop / food orders are assigned to drivers (default MANUAL). */
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "store_assignment_mode", length = 10)
    private com.bt.fretchain.primary.pojo.enums.AssignmentMode storeAssignmentMode = com.bt.fretchain.primary.pojo.enums.AssignmentMode.MANUAL;

    /** Search radius around the store for shop / food deliveries. */
    @Column(name = "store_assignment_radius_km")
    private Double storeAssignmentRadiusKm = 5.0;

    /** How freight orders get a driver: MANUAL (staff pick) or AUTO (offers go out by themselves). */
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "freight_assignment_mode", length = 10)
    private com.bt.fretchain.primary.pojo.enums.AssignmentMode freightAssignmentMode;

    @Column(name = "freight_assignment_radius_km")
    private Double freightAssignmentRadiusKm;

    /** Seconds a driver has to answer a shop / food offer (one driver at a time). */
    @Column(name = "store_offer_seconds")
    private Integer storeOfferSeconds;

    /** Seconds drivers have to answer a freight offer (several drivers at once, first to accept wins). */
    @Column(name = "freight_offer_seconds")
    private Integer freightOfferSeconds;

    /** How many nearest drivers get a freight offer at the same time. */
    @Column(name = "freight_wave_size")
    private Integer freightWaveSize;
}
