package com.bt.fretchain.security;

import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which user types are platform admins (allowed on /admin/**).
 * Configurable because existing admin accounts may use other types:
 *   security.admin-user-types=SUPER_ADMIN,ADMIN
 */
@Component
public class PanelAccessPolicy {

    private final Set<UserTypeEnum> adminTypes;

    public PanelAccessPolicy(
            @Value("${security.admin-user-types:SUPER_ADMIN,ADMIN}") String adminTypes) {
        this.adminTypes = Arrays.stream(adminTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UserTypeEnum::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserTypeEnum.class)));
    }

    public boolean isAdminType(UserTypeEnum type) {
        return type != null && adminTypes.contains(type);
    }
}
