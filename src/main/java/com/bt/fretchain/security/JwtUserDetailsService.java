package com.bt.fretchain.security;

import com.bt.fretchain.exception.UnauthorizedException;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.DriverRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolves the logged-in user from the JWT set by {@link JwtRequestFilter}.
 *
 * Tokens carry "u-id" (uuid of a system_user, or of a driver when "u-ty" = DRIVER).
 */
@Service
public class JwtUserDetailsService implements UserDetailsService {

    public static final String DRIVER_TYPE = "DRIVER";

    @Autowired
    protected SystemUserRepository systemUserRepository;

    @Autowired
    protected DriverRepository driverRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }

    /** True when the token's owner still exists (used by the JWT filter). */
    public boolean tokenOwnerExists(String userId, String userType) {
        if (userId == null) {
            return false;
        }
        if (DRIVER_TYPE.equals(userType)) {
            return driverRepository.findByUuidAndDeletedFalse(userId).isPresent();
        }
        return systemUserRepository.findByUuid(userId).filter(u -> !u.isDeleted()).isPresent();
    }

    /** The logged-in system user (admin, shop / restaurant login, customer) or 401. */
    public SystemUser getLoggedInUser() {
        return findLoggedInUser().orElseThrow(() -> new UnauthorizedException("unauthorized"));
    }

    /** The logged-in system user, or empty when the request has no valid user token (e.g. public endpoints). */
    public Optional<SystemUser> findLoggedInUser() {
        JwtAuthenticationToken jwt = currentJwt();
        if (jwt == null || DRIVER_TYPE.equals(claim(jwt, "u-ty"))) {
            return Optional.empty();
        }
        String userId = claim(jwt, "u-id");
        return userId == null ? Optional.empty()
                : systemUserRepository.findByUuid(userId).filter(u -> !u.isDeleted());
    }

    /** The logged-in driver or 401. */
    public Driver getLoggedInDriver() {
        return findLoggedInDriver().orElseThrow(() -> new UnauthorizedException("unauthorized"));
    }

    public Optional<Driver> findLoggedInDriver() {
        JwtAuthenticationToken jwt = currentJwt();
        if (jwt == null || !DRIVER_TYPE.equals(claim(jwt, "u-ty"))) {
            return Optional.empty();
        }
        String driverId = claim(jwt, "u-id");
        return driverId == null ? Optional.empty() : driverRepository.findByUuidAndDeletedFalse(driverId);
    }

    /** Null when the request is anonymous or authenticated in another way (e.g. monitoring). */
    private static JwtAuthenticationToken currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwt ? jwt : null;
    }

    private static String claim(JwtAuthenticationToken jwt, String key) {
        Object value = jwt.getTokenAttributes().get(key);
        return value == null ? null : value.toString();
    }
}
