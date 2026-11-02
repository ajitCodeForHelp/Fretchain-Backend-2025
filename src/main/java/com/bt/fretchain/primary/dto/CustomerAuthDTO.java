package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.OtpSentOnTypeEnum;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class CustomerAuthDTO {

    @Getter
    @Setter
    public static class SendOtp {

        @NotNull(message = "sentOnType is required")
        private OtpSentOnTypeEnum sentOnType; // MOBILE / EMAIL

        private String countryCode;

        @NotBlank(message = "sentTo is required")
        private String sentTo; // mobile number or email
    }

    @Getter
    @Setter
    public static class VerifyOtp {

        @NotNull(message = "sentOnType is required")
        private OtpSentOnTypeEnum sentOnType; // MOBILE / EMAIL

        private String countryCode;

        @NotBlank(message = "sentTo is required")
        private String sentTo; // mobile number or email

        @NotBlank(message = "OTP is required")
        private String otp;
    }

    @Getter
    @Setter
    public static class AuthResponse {

        private String token;

        private String userUuid;

        private String fullName;

        private String mobile;

        private String email;

        private Boolean profileCompleted;

        private UserTypeEnum userType;
    }
}