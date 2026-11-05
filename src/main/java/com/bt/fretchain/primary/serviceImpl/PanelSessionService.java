package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.primary.dto.AdminAuthDto;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * What a web-panel login is and may do, for every user type:
 *
 *   SUPER_ADMIN / ADMIN     -> admin panel, all activities
 *   ADMIN_STAFF             -> admin panel, activities of their platform role
 *   SHOP_OWNER              -> all their shops (switchable), everything in each
 *   SHOP_USER               -> one shop, activities of their shop role
 *   RESTAURANT_OWNER / USER -> same for restaurants
 *   VENDOR                  -> vendor panel: own company, vehicles and drivers
 *
 * Used at login and by GET /admin/api/auth/session (the panel refreshes it on every page).
 */
@Service
@RequiredArgsConstructor
public class PanelSessionService {

    private final PlatformAccessService platformAccess;
    private final ShopAccessService shopAccess;
    private final RestaurantAccessService restaurantAccess;
    private final VendorService vendorService;

    @Transactional(readOnly = true)
    public AdminAuthDto.UserDetails session(SystemUser user) {
        AdminAuthDto.UserDetails d = new AdminAuthDto.UserDetails();
        d.setFullName(user.getFullName());
        d.setEmail(user.getEmail());
        d.setUserType(user.getUserType());

        UserTypeEnum type = user.getUserType();
        if (platformAccess.isFullAdmin(user) || type == UserTypeEnum.ADMIN_STAFF) {
            d.setActivities(platformAccess.activityNames(user));
            d.setRoleName(type == UserTypeEnum.ADMIN_STAFF && user.getPlatformRole() != null
                    ? user.getPlatformRole().getName() : null);
        } else if (type == UserTypeEnum.SHOP_OWNER || type == UserTypeEnum.SHOP_USER) {
            var store = shopAccess.session(shopAccess.currentStaff(user));
            d.setStore(store);
            d.setStores(type == UserTypeEnum.SHOP_OWNER
                    ? shopAccess.ownedStores(user).stream().map(PanelSessionService::summary).toList()
                    : List.of(summary(store.getStoreUuid(), store.getStoreName(), store.getLogoUrl(), store.getStatus())));
        } else if (type == UserTypeEnum.RESTAURANT_OWNER || type == UserTypeEnum.RESTAURANT_USER) {
            var store = restaurantAccess.session(restaurantAccess.currentStaff(user));
            d.setStore(store);
            d.setStores(type == UserTypeEnum.RESTAURANT_OWNER
                    ? restaurantAccess.ownedStores(user).stream().map(PanelSessionService::summary).toList()
                    : List.of(summary(store.getStoreUuid(), store.getStoreName(), store.getLogoUrl(), store.getStatus())));
        } else if (type == UserTypeEnum.VENDOR) {
            // fleet owner: own company, vehicles and drivers
            d.setVendor(vendorService.session(user));
        } else {
            // customers, drivers, ... are not panel users (same message as a wrong password)
            throw new BadRequestException("Invalid Credential");
        }
        return d;
    }

    private static AdminAuthDto.StoreSummary summary(ShopMaster s) {
        return summary(s.getUuid(), s.getNameEn(), s.getLogoUrl(), s.getStatus());
    }

    private static AdminAuthDto.StoreSummary summary(RestaurantMaster r) {
        return summary(r.getUuid(), r.getNameEn(), r.getLogoUrl(), r.getStatus());
    }

    private static AdminAuthDto.StoreSummary summary(String uuid, String name, String logo, Object status) {
        AdminAuthDto.StoreSummary s = new AdminAuthDto.StoreSummary();
        s.setStoreUuid(uuid);
        s.setStoreName(name);
        s.setLogoUrl(logo);
        s.setStatus(status);
        return s;
    }
}
