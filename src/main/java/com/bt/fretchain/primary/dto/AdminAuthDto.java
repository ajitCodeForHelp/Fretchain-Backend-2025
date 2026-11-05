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
        private String email;
        /** Shop / restaurant logins only: store, role and allowed activities. */
        private Object store;
        /** Owners: all their stores (the panel lets them switch). Others: the one store. */
        private java.util.List<StoreSummary> stores;
        /** Admin panel permissions (full admins: all; admin staff: from their role). */
        private java.util.List<String> activities;
        /** Platform role name for admin staff. */
        private String roleName;
    }

    @Setter
    @Getter
    public static class StoreSummary {
        private String storeUuid;
        private String storeName;
        private String logoUrl;
        private Object status;
    }

}
