package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.primary.dto.CustomerAuthDTO;
import com.bt.fretchain.primary.pojo.OtpLog;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.OtpSentOnTypeEnum;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.pojo.enums.VerificationTypeEnum;
import com.bt.fretchain.primary.repository.OtpLogRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.service.CustomerAuthService;
import com.bt.fretchain.security.JwtTokenUtil;
import com.bt.fretchain.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private final SystemUserRepository systemUserRepository;
    private final OtpLogRepository otpLogRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public void sendOtp(CustomerAuthDTO.SendOtp request) {

        String sentTo = request.getSentTo().trim();

        otpLogRepository.expireOldOtp(
                sentTo,
                request.getSentOnType(),
                VerificationTypeEnum.LOGIN
        );


        String otpCode = "1234";
        // For production:
        // String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);

        OtpLog otpLog = new OtpLog();
        otpLog.setSentOnType(request.getSentOnType());
        otpLog.setSentTo(sentTo);
        otpLog.setOtpCode(otpCode);
        otpLog.setVerificationType(VerificationTypeEnum.LOGIN);
        otpLog.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otpLog.setExpired(false);
        otpLog.setVerified(false);
        otpLog.setAttemptCount(0);

        otpLogRepository.save(otpLog);

        if (request.getSentOnType() == OtpSentOnTypeEnum.MOBILE) {
            log.info("Customer mobile OTP sent to {} : {}", sentTo, otpCode);
            // TODO call SMS / WhatsApp API
        } else {
            log.info("Customer email OTP sent to {} : {}", sentTo, otpCode);
            // TODO call Email API
        }
    }

    @Override
    public CustomerAuthDTO.AuthResponse verifyOtp(CustomerAuthDTO.VerifyOtp request) {

        String sentTo = request.getSentTo().trim();

        OtpLog otpLog = otpLogRepository
                .findBySentToAndOtpCodeAndExpiredFalseAndVerifiedFalse(
                        sentTo,
                        request.getOtp()
                )
                .orElseThrow(() -> new BadRequestException("invalid.otp"));

        if (otpLog.getExpiredAt().isBefore(LocalDateTime.now())) {
            otpLog.setExpired(true);
            otpLogRepository.save(otpLog);
            throw new BadRequestException("otp.expired");
        }

        otpLog.setVerified(true);
        otpLog.setExpired(true);
        otpLog.setVerifiedAt(LocalDateTime.now());
        otpLogRepository.save(otpLog);

        SystemUser customer = getOrCreateCustomer(request);

        customer.setLastLogin(LocalDateTime.now());

        if (request.getSentOnType() == OtpSentOnTypeEnum.MOBILE) {
            customer.setMobileVerified(true);
        } else {
            customer.setEmailVerified(true);
        }

        if (TextUtils.isEmpty(customer.getUniqueKey())) {
            customer.setUniqueKey(TextUtils.getUniqueKey());
        }

        systemUserRepository.save(customer);

        return generateAuthResponse(customer);
    }

    private SystemUser getOrCreateCustomer(CustomerAuthDTO.VerifyOtp request) {

        String sentTo = request.getSentTo().trim();

        if (request.getSentOnType() == OtpSentOnTypeEnum.MOBILE) {
            return systemUserRepository
                    .findByMobileAndDeletedFalse(sentTo)
                    .orElseGet(() -> createCustomerByMobile(request));
        }

        return systemUserRepository
                .findByEmailAndDeletedFalse(sentTo)
                .orElseGet(() -> createCustomerByEmail(request));
    }

    private SystemUser createCustomerByMobile(CustomerAuthDTO.VerifyOtp request) {

        SystemUser user = new SystemUser();
        user.setMobile(request.getSentTo().trim());
        user.setMobileVerified(true);
        user.setUserType(UserTypeEnum.CUSTOMER);
        user.setActive(true);
        user.setUniqueKey(TextUtils.getUniqueKey());

        return systemUserRepository.save(user);
    }

    private SystemUser createCustomerByEmail(CustomerAuthDTO.VerifyOtp request) {

        SystemUser user = new SystemUser();
        user.setEmail(request.getSentTo().trim());
        user.setEmailVerified(true);
        user.setUserType(UserTypeEnum.CUSTOMER);
        user.setActive(true);
        user.setUniqueKey(TextUtils.getUniqueKey());

        return systemUserRepository.save(user);
    }

    private CustomerAuthDTO.AuthResponse generateAuthResponse(SystemUser user) {

        CustomerAuthDTO.AuthResponse response = new CustomerAuthDTO.AuthResponse();

        String token = UUID.randomUUID().toString();

        Map<String, Object> bodyPart = new LinkedHashMap<>();
        bodyPart.put("username", user.getUsername());
        bodyPart.put("u-id", user.getUuid());
        bodyPart.put("u-ty", user.getUserType().toString());
        bodyPart.put("roles", List.of(user.getUserType().toString()));
        bodyPart.put("d-id", user.getUuid());
        bodyPart.put("token", token);

        response.setToken(jwtTokenUtil.generateToken(bodyPart, user));
        response.setUserUuid(user.getUuid());
        response.setFullName(user.getFullName());
        response.setMobile(user.getMobile());
        response.setEmail(user.getEmail());
        response.setProfileCompleted(!TextUtils.isEmpty(user.getFullName()));

        return response;
    }
}