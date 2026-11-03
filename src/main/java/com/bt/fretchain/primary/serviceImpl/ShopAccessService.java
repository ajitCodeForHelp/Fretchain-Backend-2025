package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopPartnerDTO;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.ShopRole;
import com.bt.fretchain.primary.pojo.ShopStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.ShopStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Resolves which shop a login belongs to and whether it may perform an activity.
 * Every /partner/api/shop-* endpoint goes through {@link #require}.
 */
@Service
@RequiredArgsConstructor
public class ShopAccessService {

    private final ShopStaffRepository staffRepository;

    /** The staff record of a SHOP_USER login, or 403. */
    public ShopStaff currentStaff(SystemUser user) {
        if (user == null || user.getUserType() != UserTypeEnum.SHOP_USER) {
            throw new ForbiddenException("access.denied");
        }
        ShopStaff staff = staffRepository.findByUser_RecordIdAndDeletedFalse(user.getRecordId())
                .orElseThrow(() -> new ForbiddenException("access.denied"));

        if (!staff.isActive()) {
            throw new ForbiddenException("staff.inactive");
        }
        ShopMaster shop = staff.getShop();
        if (shop.isDeleted() || !shop.isActive()) {
            throw new ForbiddenException("store.inactive");
        }
        return staff;
    }

    /** Effective activities: the owner has all; an inactive role has none. */
    public Set<ShopActivity> activities(ShopStaff staff) {
        ShopRole role = staff.getRole();
        if (staff.isOwner() || (role != null && role.isOwnerRole())) {
            return EnumSet.allOf(ShopActivity.class);
        }
        if (role == null || role.isDeleted() || !role.isActive() || role.getActivities().isEmpty()) {
            return EnumSet.noneOf(ShopActivity.class);
        }
        return EnumSet.copyOf(role.getActivities());
    }

    public boolean has(ShopStaff staff, ShopActivity activity) {
        Set<ShopActivity> granted = activities(staff);
        if (granted.contains(activity)) {
            return true;
        }
        return granted.stream().anyMatch(a -> a.getGroup().equals(activity.getGroup())
                // any write action implies VIEW of the same menu
                && ((!activity.isWrite() && a.isWrite())
                // legacy MANAGE implies ADD / EDIT / STATUS
                || (activity.isWrite() && a.isLegacy())));
    }

    /** Current staff, checked for the activity. Write actions are blocked for suspended shops. */
    public ShopStaff require(SystemUser user, ShopActivity activity) {
        ShopStaff staff = currentStaff(user);
        if (!has(staff, activity)) {
            throw new ForbiddenException("access.denied");
        }
        if (activity.isWrite() && staff.getShop().getStatus() == ShopStatus.SUSPENDED) {
            throw new ForbiddenException("store.suspended");
        }
        return staff;
    }

    /** Current staff with at least one of the activities (e.g. a dropdown used by several screens). */
    public ShopStaff requireAny(SystemUser user, ShopActivity... activities) {
        ShopStaff staff = currentStaff(user);
        for (ShopActivity a : activities) {
            if (has(staff, a)) {
                return staff;
            }
        }
        throw new ForbiddenException("access.denied");
    }

    /** A record of another shop is reported as "not found" rather than leaking its existence. */
    public void ensureSameShop(String recordShopUuid, ShopStaff staff) {
        if (recordShopUuid == null || !recordShopUuid.equals(staff.getShop().getUuid())) {
            throw new ResourceNotFoundException("record.notfound");
        }
    }

    public ShopPartnerDTO.Session session(ShopStaff staff) {
        ShopMaster shop = staff.getShop();
        ShopPartnerDTO.Session session = new ShopPartnerDTO.Session();
        session.setStoreUuid(shop.getUuid());
        session.setStoreName(shop.getNameEn());
        session.setStoreNameFr(shop.getNameFr());
        session.setLogoUrl(shop.getLogoUrl());
        session.setStatus(shop.getStatus());
        session.setStatusReason(shop.getStatusReason());
        session.setOwner(staff.isOwner());
        session.setRoleName(staff.getRole() != null ? staff.getRole().getName() : null);

        // Expose VIEW activities implied by MANAGE so the UI can rely on the list alone
        List<ShopActivity> list = new ArrayList<>();
        for (ShopActivity a : ShopActivity.values()) {
            if (!a.isLegacy() && has(staff, a)) {
                list.add(a);
            }
        }
        session.setActivities(list);
        session.setGlobalCategories(shop.getGlobalCategories().stream()
                .filter(g -> g.isActive() && !g.isDeleted())
                .map(g -> new KeyValueDto(g.getUuid(), g.getNameEn()))
                .toList());
        return session;
    }
}
