package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Creates / updates the SystemUser behind a shop or restaurant login
 * (owner or staff). Shared by both modules because it only deals with users.
 */
@Component
@RequiredArgsConstructor
public class StoreLoginHelper {

    private final SystemUserRepository userRepository;

    public SystemUser create(String fullName, String email, String mobile, String password,
                             UserTypeEnum type, Long createdBy) {

        if (password == null || password.isBlank()) {
            throw new BadRequestException("password.required");
        }
        String normalizedEmail = normalizeEmail(email);
        String normalizedMobile = blankToNull(mobile);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("user.email.exists");
        }
        if (normalizedMobile != null && userRepository.existsByMobile(normalizedMobile)) {
            throw new DuplicateResourceException("user.mobile.exists");
        }

        SystemUser user = new SystemUser();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setMobile(normalizedMobile);
        user.setPassword(TextUtils.getEncodedPassword(password));
        user.setUserType(type);
        user.setUniqueKey(TextUtils.getUniqueKey());
        user.setCreatedBy(createdBy);
        user.setModifiedBy(createdBy);
        return userRepository.save(user);
    }

    public SystemUser update(SystemUser user, String fullName, String email, String mobile,
                             String password, Long modifiedBy) {

        String normalizedEmail = normalizeEmail(email);
        String normalizedMobile = blankToNull(mobile);

        if (userRepository.existsByEmailIgnoreCaseAndRecordIdNot(normalizedEmail, user.getRecordId())) {
            throw new DuplicateResourceException("user.email.exists");
        }
        if (normalizedMobile != null
                && userRepository.existsByMobileAndRecordIdNot(normalizedMobile, user.getRecordId())) {
            throw new DuplicateResourceException("user.mobile.exists");
        }

        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setMobile(normalizedMobile);
        if (password != null && !password.isBlank()) {
            user.setPassword(TextUtils.getEncodedPassword(password));
            // invalidate other sessions on password change
            user.setUniqueKey(TextUtils.getUniqueKey());
        }
        user.setModifiedBy(modifiedBy);
        return userRepository.save(user);
    }

    public void setActive(SystemUser user, boolean active, Long modifiedBy) {
        user.setActive(active);
        user.setModifiedBy(modifiedBy);
        userRepository.save(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
