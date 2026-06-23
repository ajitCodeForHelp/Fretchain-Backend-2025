package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.OtpSentOnTypeEnum;
import com.bt.fretchain.primary.pojo.enums.VerificationTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_logs")
@Getter
@Setter
public class OtpLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "sent_on_type", nullable = false)
    private OtpSentOnTypeEnum sentOnType;

    @Column(name = "sent_to", nullable = false, length = 200)
    private String sentTo;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false)
    private VerificationTypeEnum verificationType;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "expired")
    private boolean expired = false;

    @Column(name = "verified")
    private boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "reference_id")
    private String referenceId;
}