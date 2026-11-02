package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import jakarta.validation.constraints.NotBlank;
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

        private String vehicleUuid;

        private String vehicleNumber;

        private String vehicleCategoryUuid;

        private String vehicleCategoryNameEn;

        private String vehicleCategoryNameFr;
    }
}