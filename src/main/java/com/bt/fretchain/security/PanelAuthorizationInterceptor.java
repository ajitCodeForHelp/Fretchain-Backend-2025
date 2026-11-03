package com.bt.fretchain.security;

import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Role check for the web panel APIs (runs after JWT authentication):
 *   /admin/**                     -> platform admins only (except /admin/api/auth/**)
 *   /partner/api/shop-*           -> SHOP_USER logins
 *   /partner/api/restaurant-*     -> RESTAURANT_USER logins
 * Fine-grained activity checks happen in ShopAccessService / RestaurantAccessService.
 * Also rejects tokens of users that were deactivated or deleted after login.
 */
@Component
@RequiredArgsConstructor
public class PanelAuthorizationInterceptor implements HandlerInterceptor {

    private final JwtUserDetailsService userDetailsService;
    private final PanelAccessPolicy policy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        boolean adminPath = path.startsWith("/admin/") && !path.startsWith("/admin/api/auth/");
        boolean partnerPath = path.startsWith("/partner/");
        if (!adminPath && !partnerPath) {
            return true;
        }

        SystemUser user;
        try {
            user = userDetailsService.getLoggedInUser();
        } catch (Exception e) {
            throw new ForbiddenException("access.denied");
        }
        if (user == null || !user.isActive() || user.isDeleted()) {
            throw new ForbiddenException("account.inactive");
        }

        UserTypeEnum type = user.getUserType();
        if (adminPath) {
            if (!policy.isAdminType(type)) {
                throw new ForbiddenException("access.denied");
            }
        } else if (path.startsWith("/partner/api/shop-")) {
            if (type != UserTypeEnum.SHOP_USER) {
                throw new ForbiddenException("access.denied");
            }
        } else if (path.startsWith("/partner/api/restaurant-")) {
            if (type != UserTypeEnum.RESTAURANT_USER) {
                throw new ForbiddenException("access.denied");
            }
        } else {
            throw new ForbiddenException("access.denied");
        }
        return true;
    }
}
