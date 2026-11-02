package com.bt.fretchain.primary.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class SettingsDTO {

    @Getter
    @Setter
    public static class Pricing {

        private BigDecimal adminServiceCharge;
        private BigDecimal insurancePercentage;
        private BigDecimal helperCharge;
        private BigDecimal waitingChargePerMinute;
        private BigDecimal taxPercentage;
        private BigDecimal cashServicePercentage;
    }

    @Getter
    @Setter
    public static class Platform {

        private String companyName;
        private String logoUrl;
        private String faviconUrl;
        private String companyAddress;

        private String supportEmail;
        private String supportPhone;
        private String supportWhatsapp;

        private String currencyCode;
        private String currencySymbol;

        private Boolean platformMaintenanceMode;
        private String privacyPolicyUrl;
        private String termsConditionUrl;

        private String facebookUrl;
        private String instagramUrl;
        private String linkedinUrl;
        private String youtubeUrl;
        private String twitterUrl;
    }

    @Getter
    @Setter
    public static class Payment {

        private Boolean paymentTestMode;
        private Boolean paymentMaintenanceMode;
        private Boolean cashEnabled;
        private Boolean cardEnabled;
        private Boolean mobileMoneyEnabled;
        private Boolean bankTransferEnabled;
        private Boolean walletEnabled;
    }

    @Getter
    @Setter
    public static class Notification {

        private Boolean smsEnabled;
        private Boolean emailEnabled;
        private Boolean pushEnabled;
        private Boolean whatsappEnabled;
        private Boolean promotionalNotificationEnabled;
        private Boolean orderStatusNotificationEnabled;
        private Boolean driverAssignmentNotificationEnabled;
        private Boolean paymentNotificationEnabled;
    }
}