package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class AdminAuthDto {

    @Setter
    @Getter
    public static class Login {
        @NotNull
        private String userName;
        @NotNull
        private String password;
    }

    @Setter
    @Getter
    public static class UserDetails {
        private String fullName;
        private String secretKey;
        private UserTypeEnum userType;
    }

}
