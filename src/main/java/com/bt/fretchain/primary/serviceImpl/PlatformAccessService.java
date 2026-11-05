package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.pojo.PlatformRole;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.PlatformActivity;
import com.bt.fretchain.primary.pojo.enums.PlatformActivity.Action;
import com.bt.fretchain.primary.pojo.enums.PlatformActivity.Menu;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.security.PanelAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Admin-panel permissions. SUPER_ADMIN / ADMIN (see PanelAccessPolicy) have everything;
 * ADMIN_STAFF get the activities of their PlatformRole (+ VIEW implied by any action).
 */
@Service
@RequiredArgsConstructor
public class PlatformAccessService {

    private final PanelAccessPolicy policy;

    public boolean isFullAdmin(SystemUser user) {
        return user != null && policy.isAdminType(user.getUserType());
    }

    public Set<PlatformActivity> activities(SystemUser user) {
        if (isFullAdmin(user)) {
            return EnumSet.allOf(PlatformActivity.class);
        }
        if (user == null || user.getUserType() != UserTypeEnum.ADMIN_STAFF) {
            return EnumSet.noneOf(PlatformActivity.class);
        }
        PlatformRole role = user.getPlatformRole();
        if (role == null || role.isDeleted() || !role.isActive() || role.getActivities().isEmpty()) {
            return EnumSet.noneOf(PlatformActivity.class);
        }
        EnumSet<PlatformActivity> result = EnumSet.copyOf(role.getActivities());
        for (PlatformActivity a : role.getActivities()) {
            result.add(PlatformActivity.of(a.getMenu(), Action.VIEW)); // any action implies VIEW
        }
        return result;
    }

    public boolean has(SystemUser user, PlatformActivity activity) {
        return activities(user).contains(activity);
    }

    /**
     * Decide whether this admin-panel request is allowed for the user.
     * path = "/admin/api/{resource}/..."
     */
    public boolean isAllowed(SystemUser user, String method, String path) {
        if (isFullAdmin(user)) {
            return true;
        }
        if (user.getUserType() != UserTypeEnum.ADMIN_STAFF) {
            return false;
        }
        Set<PlatformActivity> granted = activities(user);
        if (granted.isEmpty()) {
            return false;
        }

        String[] parts = path.split("/");      // "", "admin", "api", resource, ...
        if (parts.length < 4) {
            return false;
        }
        // Dropdown data (names only) is needed by many screens: any staff may read it
        if (path.endsWith("/list-in-key-value") && "GET".equalsIgnoreCase(method)) {
            return true;
        }
        Menu menu = Menu.forResource(parts[3]);
        if (menu == null) {
            return false;   // unknown admin resource: full admins only
        }
        return granted.contains(PlatformActivity.of(menu, actionFor(method, path)));
    }

    static Action actionFor(String method, String path) {
        switch (method.toUpperCase(Locale.ROOT)) {
            case "GET":
                return Action.VIEW;
            case "POST":
                if (path.endsWith("/list") || path.contains("/list/")) return Action.VIEW;
                if (path.endsWith("/save")) return Action.ADD;
                return Action.EDIT;
            case "PUT":
                return Action.EDIT;
            default:                     // PATCH (status / approval), DELETE
                return Action.STATUS;
        }
    }

    /** Names for the UI. */
    public List<String> activityNames(SystemUser user) {
        return activities(user).stream().map(Enum::name).sorted().toList();
    }
}
