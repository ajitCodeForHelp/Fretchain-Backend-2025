package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantPartnerDTO;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import com.bt.fretchain.primary.pojo.RestaurantRole;
import com.bt.fretchain.primary.pojo.RestaurantStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.RestaurantActivity;
import com.bt.fretchain.primary.pojo.enums.RestaurantStatus;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.RestaurantStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Resolves which restaurant a login belongs to and whether it may perform an activity.
 * Every /partner/api/restaurant-* endpoint goes through {@link #require}.
 */
@Service
@RequiredArgsConstructor
public class RestaurantAccessService {

    private final RestaurantStaffRepository staffRepository;

    /** The staff record of a RESTAURANT_USER login, or 403. */
    public RestaurantStaff currentStaff(SystemUser user) {
        if (user == null || user.getUserType() != UserTypeEnum.RESTAURANT_USER) {
            throw new ForbiddenException("access.denied");
        }
        RestaurantStaff staff = staffRepository.findByUser_RecordIdAndDeletedFalse(user.getRecordId())
                .orElseThrow(() -> new ForbiddenException("access.denied"));

        if (!staff.isActive()) {
            throw new ForbiddenException("staff.inactive");
        }
        RestaurantMaster restaurant = staff.getRestaurant();
        if (restaurant.isDeleted() || !restaurant.isActive()) {
            throw new ForbiddenException("store.inactive");
        }
        return staff;
    }

    /** Effective activities: the owner has all; an inactive role has none. */
    public Set<RestaurantActivity> activities(RestaurantStaff staff) {
        RestaurantRole role = staff.getRole();
        if (staff.isOwner() || (role != null && role.isOwnerRole())) {
            return EnumSet.allOf(RestaurantActivity.class);
        }
        if (role == null || role.isDeleted() || !role.isActive() || role.getActivities().isEmpty()) {
            return EnumSet.noneOf(RestaurantActivity.class);
        }
        return EnumSet.copyOf(role.getActivities());
    }

    public boolean has(RestaurantStaff staff, RestaurantActivity activity) {
        Set<RestaurantActivity> granted = activities(staff);
        if (granted.contains(activity)) {
            return true;
        }
        return granted.stream().anyMatch(a -> a.getGroup().equals(activity.getGroup())
                // any write action implies VIEW of the same menu
                && ((!activity.isWrite() && a.isWrite())
                // legacy MANAGE implies ADD / EDIT / STATUS
                || (activity.isWrite() && a.isLegacy())));
    }

    /** Current staff, checked for the activity. Write actions are blocked for suspended restaurants. */
    public RestaurantStaff require(SystemUser user, RestaurantActivity activity) {
        RestaurantStaff staff = currentStaff(user);
        if (!has(staff, activity)) {
            throw new ForbiddenException("access.denied");
        }
        if (activity.isWrite() && staff.getRestaurant().getStatus() == RestaurantStatus.SUSPENDED) {
            throw new ForbiddenException("store.suspended");
        }
        return staff;
    }

    /** A record of another restaurant is reported as "not found" rather than leaking its existence. */
    public void ensureSameRestaurant(String recordRestaurantUuid, RestaurantStaff staff) {
        if (recordRestaurantUuid == null || !recordRestaurantUuid.equals(staff.getRestaurant().getUuid())) {
            throw new ResourceNotFoundException("record.notfound");
        }
    }

    public RestaurantPartnerDTO.Session session(RestaurantStaff staff) {
        RestaurantMaster restaurant = staff.getRestaurant();
        RestaurantPartnerDTO.Session session = new RestaurantPartnerDTO.Session();
        session.setStoreUuid(restaurant.getUuid());
        session.setStoreName(restaurant.getNameEn());
        session.setStoreNameFr(restaurant.getNameFr());
        session.setLogoUrl(restaurant.getLogoUrl());
        session.setStatus(restaurant.getStatus());
        session.setStatusReason(restaurant.getStatusReason());
        session.setOwner(staff.isOwner());
        session.setRoleName(staff.getRole() != null ? staff.getRole().getName() : null);

        // Expose VIEW activities implied by MANAGE so the UI can rely on the list alone
        List<RestaurantActivity> list = new ArrayList<>();
        for (RestaurantActivity a : RestaurantActivity.values()) {
            if (!a.isLegacy() && has(staff, a)) {
                list.add(a);
            }
        }
        session.setActivities(list);
        session.setGlobalCategories(restaurant.getGlobalCategories().stream()
                .filter(g -> g.isActive() && !g.isDeleted())
                .map(g -> new KeyValueDto(g.getUuid(), g.getNameEn()))
                .toList());
        return session;
    }
}
