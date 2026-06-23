package com.bt.fretchain.primary.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "driver_documents",
        indexes = {
                @Index(name = "idx_driver_document_driver", columnList = "driver_id")
        }
)
@Getter
@Setter
public class DriverDocument extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver;

    @Column(name = "identity_number", length = 50)
    private String identityNumber;

    @Column(name = "identity_document_url", length = 500)
    private String identityDocumentUrl;

    @Column(name = "license_image_url", length = 500)
    private String licenseImageUrl;

    @Column(name = "police_verification_url", length = 500)
    private String policeVerificationUrl;

    @Column(name = "address_proof_url", length = 500)
    private String addressProofUrl;

    @Column(name = "contract_document_url", length = 500)
    private String contractDocumentUrl;

    @Column(name = "medical_certificate_url", length = 500)
    private String medicalCertificateUrl;

    @Column(name = "license_validity")
    private LocalDate licenseValidity;

    @Column(name = "reference_name", length = 120)
    private String referenceName;

    @Column(name = "reference_mobile", length = 20)
    private String referenceMobile;
}