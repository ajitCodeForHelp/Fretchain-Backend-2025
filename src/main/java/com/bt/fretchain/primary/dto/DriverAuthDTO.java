package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class DriverAuthDTO {

    @Getter
    @Setter
    public static class Login {

        @NotBlank(message = "Username is required")
        private String userName;

        @NotBlank(message = "Password is required")
        private String password;

        /** Optional: device + location details captured at login so we can push orders to this phone. */
        private DeviceInfo device;
    }

    @Getter
    @Setter
    public static class DeviceInfo {

        /** FCM registration token. */
        private String fcmToken;

        private String deviceId;

        /** android | ios */
        private String platform;

        private String deviceModel;

        private String appVersion;

        private Double latitude;

        private Double longitude;
    }

    @Getter
    @Setter
    public static class LocationUpdate {

        @NotNull(message = "Latitude is required")
        private Double latitude;

        @NotNull(message = "Longitude is required")
        private Double longitude;
    }

    @Getter
    @Setter
    public static class OnlineUpdate {

        @NotNull(message = "Online flag is required")
        private Boolean online;

        private Double latitude;

        private Double longitude;
    }


    @Getter
    @Setter
    public static class AuthResponse {

        private String token;

        private String driverUuid;

        private String fullName;

        private String mobile;

        private String email;

        private String profileImageUrl;

        private Boolean verified;

        private Boolean available;

        private Boolean onlineStatus;

        private java.math.BigDecimal rating;

        private String vehicleUuid;

        private String vehicleNumber;

        private String vehicleCategoryUuid;

        private String vehicleCategoryNameEn;

        private String vehicleCategoryNameFr;
    }
}