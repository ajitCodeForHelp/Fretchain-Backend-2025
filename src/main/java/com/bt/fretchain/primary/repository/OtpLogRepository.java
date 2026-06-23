package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.OtpLog;
import com.bt.fretchain.primary.pojo.enums.OtpSentOnTypeEnum;
import com.bt.fretchain.primary.pojo.enums.VerificationTypeEnum;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OtpLogRepository extends JpaRepository<OtpLog, Long> {

    Optional<OtpLog> findBySentToAndOtpCodeAndExpiredFalseAndVerifiedFalse(
            String sentTo,
            String otpCode
    );

    @Modifying
    @Transactional
    @Query("""
       update OtpLog o
       set o.expired = true
       where o.sentTo = :sentTo
       and o.sentOnType = :sentOnType
       and o.verificationType = :verificationType
       and o.expired = false
       and o.verified = false
       """)
    void expireOldOtp(
            @Param("sentTo") String sentTo,
            @Param("sentOnType") OtpSentOnTypeEnum sentOnType,
            @Param("verificationType") VerificationTypeEnum verificationType
    );
}