package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class Vehicle extends BaseEntity {

    @Column(name = "vehicle_number", nullable = false, unique = true, length = 50)
    private String vehicleNumber;

    @Column(name = "rc_number", length = 100)
    private String rcNumber;

    @Column(name = "fitness_certificate_number", length = 100)
    private String fitnessCertificateNumber;

    @Column(name = "chassis_number", length = 150)
    private String chassisNumber;

    @Column(name = "max_weight_kg")
    private Long maxWeightKg;

    @Column(name = "verified")
    private Boolean verified = false;

    /** PENDING / VERIFIED / REJECTED, same review process as drivers. */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    private com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus verificationStatus;

    @Column(name = "verification_reason", length = 500)
    private String verificationReason;

    @Column(name = "verification_changed_at")
    private java.time.LocalDateTime verificationChangedAt;

    @Column(name = "verification_reviewer", length = 120)
    private String verificationReviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_category_id")
    private VehicleCategory vehicleCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private SystemUser vendor;

    @OneToOne(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private Driver driver;

    public com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus getVerificationStatus() {
        if (verificationStatus != null) {
            return verificationStatus;
        }
        return Boolean.TRUE.equals(verified)
                ? com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED
                : com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.PENDING;
    }

    /** Keeps the boolean flag (used by dispatch) in step with the review status. */
    public void review(com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus status, String reason, String reviewer) {
        this.verificationStatus = status;
        this.verified = status == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED;
        this.verificationReason = status == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.REJECTED ? reason : null;
        this.verificationChangedAt = java.time.LocalDateTime.now();
        this.verificationReviewer = reviewer;
    }

    /** After a form save: the "verified" tick on the vehicle form still works and moves the review status with it. */
    public void syncReviewWithFlag(String reviewer) {
        if (Boolean.TRUE.equals(verified)) {
            if (verificationStatus != com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED) {
                review(com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED, null, reviewer);
            }
        } else if (verificationStatus == null || verificationStatus == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED) {
            review(com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.PENDING, null, null);
        }
    }
}
