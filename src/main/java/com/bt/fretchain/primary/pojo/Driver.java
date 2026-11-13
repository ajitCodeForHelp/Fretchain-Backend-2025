package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.GenderTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "drivers",
        indexes = {
                @Index(name = "idx_driver_mobile", columnList = "mobile"),
                @Index(name = "idx_driver_available", columnList = "available"),
                @Index(name = "idx_driver_username", columnList = "username"),
                @Index(name = "idx_driver_vendor", columnList = "vendor_id")
        }
)
@Getter
@Setter
public class Driver extends BaseEntity  {

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "mobile", nullable = false, unique = true, length = 20)
    private String mobile;

    @Column(name = "country_code", length = 10)
    private String countryCode; // +91, +237, +27

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "username", unique = true, length = 50)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private GenderTypeEnum gender;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "national_id_number", length = 100)
    private String nationalIdNumber;

    @Column(name = "verified")
    private Boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    private com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus verificationStatus;

    /** Why the account was rejected (shown to the driver / fleet owner). */
    @Column(name = "verification_reason", length = 500)
    private String verificationReason;

    @Column(name = "verification_changed_at")
    private LocalDateTime verificationChangedAt;

    @Column(name = "verification_reviewer", length = 120)
    private String verificationReviewer;

    @Column(name = "available")
    private Boolean available = true;

    @Column(name = "online_status")
    private Boolean onlineStatus = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "overall_rating", precision = 2, scale = 1)
    private BigDecimal overallRating = BigDecimal.ZERO;

    @Column(name = "wallet_balance", precision = 14, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(name = "bonus_points")
    private Long bonusPoints = 0L;

    @Column(name = "device_token", length = 1000)
    private String deviceToken;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "device_platform", length = 20)
    private String devicePlatform; // android | ios

    @Column(name = "device_model", length = 120)
    private String deviceModel;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "device_updated_at")
    private LocalDateTime deviceUpdatedAt;

    @Column(name = "current_latitude")
    private Double currentLat;

    @Column(name = "current_longitude")
    private Double currentLng;

    @Column(name = "last_location_updated_at")
    private LocalDateTime lastLocationUpdatedAt;

    @Column(name = "joining_date")
    private LocalDateTime joiningDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private SystemUser vendor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @OneToOne(
            mappedBy = "driver",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private DriverDocument driverDocument;

    /** PENDING / VERIFIED / REJECTED; older rows without a stored status are derived from "verified". */
    public com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus getVerificationStatus() {
        if (verificationStatus != null) {
            return verificationStatus;
        }
        return Boolean.TRUE.equals(verified)
                ? com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED
                : com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.PENDING;
    }

    /** Keeps the boolean flag (used by dispatch and login) in step with the review status. */
    public void review(com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus status, String reason, String reviewer) {
        this.verificationStatus = status;
        this.verified = status == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED;
        this.verificationReason = status == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.REJECTED ? reason : null;
        this.verificationChangedAt = LocalDateTime.now();
        this.verificationReviewer = reviewer;
        if (status != com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED) {
            this.onlineStatus = false;
        }
    }
}
