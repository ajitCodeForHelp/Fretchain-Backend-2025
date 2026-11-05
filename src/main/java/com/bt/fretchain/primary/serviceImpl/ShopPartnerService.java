package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.dto.ShopPartnerDTO;
import com.bt.fretchain.primary.mapper.ShopMasterMapper;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.ShopMasterRepository;
import com.bt.fretchain.primary.repository.ShopRoleRepository;
import com.bt.fretchain.primary.repository.ShopStaffRepository;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Shop self-service: own profile, roles (activity mapping) and staff logins,
 * plus the admin action that creates the shop owner login.
 */
@Service
@RequiredArgsConstructor
public class ShopPartnerService {

    private static final String OWNER_ROLE_NAME = "Owner";

    private final ShopAccessService access;
    private final ShopMasterRepository shopRepository;
    private final com.bt.fretchain.primary.repository.SystemUserRepository userRepository;
    private final ShopRoleRepository roleRepository;
    private final ShopStaffRepository staffRepository;
    private final ShopMasterMapper shopMapper;
    private final StoreLoginHelper loginHelper;

    // =====================================================================
    // Profile
    // =====================================================================

    @Transactional(readOnly = true)
    public ShopPartnerDTO.Session session(SystemUser user) {
        return access.session(access.currentStaff(user));
    }

    @Transactional(readOnly = true)
    public ShopMasterDTO.Detail getProfile(SystemUser user) {
        ShopStaff staff = access.require(user, ShopActivity.PROFILE_VIEW);
        return shopMapper.toDetailDto(staff.getShop());
    }

    @Transactional
    public ShopMasterDTO.Detail updateProfile(SystemUser user, ShopPartnerDTO.ProfileUpdate req) {
        ShopStaff staff = access.require(user, ShopActivity.PROFILE_EDIT);

        if ((req.getOpeningTime() == null) != (req.getClosingTime() == null)
                || (req.getOpeningTime() != null && req.getOpeningTime().equals(req.getClosingTime()))) {
            throw new BadRequestException("shop.timing.invalid");
        }

        ShopMaster shop = staff.getShop();
        shop.setNameEn(req.getNameEn().trim());
        shop.setNameFr(req.getNameFr());
        shop.setDescriptionEn(req.getDescriptionEn());
        shop.setDescriptionFr(req.getDescriptionFr());
        shop.setCountryCode(req.getCountryCode());
        shop.setMobile(req.getMobile());
        shop.setEmail(req.getEmail());
        shop.setLogoUrl(req.getLogoUrl());
        shop.setBannerUrl(req.getBannerUrl());
        shop.setAddress(req.getAddress());
        shop.setCity(req.getCity());
        shop.setState(req.getState());
        shop.setCountry(req.getCountry());
        shop.setPostalCode(req.getPostalCode());
        shop.setLatitude(req.getLatitude());
        shop.setLongitude(req.getLongitude());
        shop.setDeliveryRadiusKm(req.getDeliveryRadiusKm());
        shop.setOpeningTime(req.getOpeningTime());
        shop.setClosingTime(req.getClosingTime());
        shop.setAcceptingOrders(req.getAcceptingOrders() == null || req.getAcceptingOrders());
        shop.setAvgPreparationMinutes(req.getAvgPreparationMinutes());
        shop.setMinOrderAmount(req.getMinOrderAmount());
        shop.setDeliveryFee(req.getDeliveryFee());
        shop.setModifiedBy(user.getRecordId());

        return shopMapper.toDetailDto(shopRepository.save(shop));
    }

    // =====================================================================
    // Roles (activity mapping)
    // =====================================================================

    public List<ShopPartnerDTO.ActivityOption> activityCatalog() {
        List<ShopPartnerDTO.ActivityOption> list = new ArrayList<>();
        for (ShopActivity a : ShopActivity.values()) {
            if (a.isLegacy()) {
                continue;
            }
            ShopPartnerDTO.ActivityOption o = new ShopPartnerDTO.ActivityOption();
            o.setCode(a);
            o.setGroup(a.getGroup());
            o.setWrite(a.isWrite());
            o.setAction(a.getAction().name());
            list.add(o);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public DataTableResponsePacket<ShopPartnerDTO.RoleDetail> listRoles(
            SystemUser user, ShopPartnerDTO.ListOrSearchData req) {

        ShopStaff staff = access.require(user, ShopActivity.ROLE_VIEW);
        Long shopId = staff.getShop().getRecordId();

        Specification<ShopRole> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("shop").get("recordId"), shopId));
            p.add(cb.isFalse(root.get("deleted")));
            String pattern = SearchUtils.containsPattern(req.getSearch());
            if (pattern != null) {
                p.add(cb.like(cb.lower(root.get("name")), pattern, SearchUtils.LIKE_ESCAPE));
            }
            if (req.getFilters() != null && req.getFilters().getActive() != null) {
                p.add(cb.equal(root.get("active"), req.getFilters().getActive()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };

        return PaginationUtils.createResponse(
                roleRepository.findAll(spec, page(req, Set.of("name", "createdAt"), "name"))
                        .map(this::toRoleDetail));
    }

    @Transactional(readOnly = true)
    public ShopPartnerDTO.RoleDetail getRole(SystemUser user, String uuid) {
        ShopStaff staff = access.require(user, ShopActivity.ROLE_VIEW);
        return toRoleDetail(findRole(uuid, staff));
    }

    @Transactional
    public ShopPartnerDTO.RoleDetail saveRole(SystemUser user, ShopPartnerDTO.RoleSaveOrUpdate req) {
        ShopStaff staff = access.require(user, ShopActivity.ROLE_ADD);
        Long shopId = staff.getShop().getRecordId();

        if (roleRepository.existsByShop_RecordIdAndNameIgnoreCaseAndDeletedFalse(shopId, req.getName().trim())) {
            throw new DuplicateResourceException("role.name.exists");
        }

        ShopRole role = new ShopRole();
        role.setShop(staff.getShop());
        applyRole(role, req);
        role.setCreatedBy(user.getRecordId());
        role.setModifiedBy(user.getRecordId());
        return toRoleDetail(roleRepository.save(role));
    }

    @Transactional
    public ShopPartnerDTO.RoleDetail updateRole(SystemUser user, String uuid, ShopPartnerDTO.RoleSaveOrUpdate req) {
        ShopStaff staff = access.require(user, ShopActivity.ROLE_EDIT);
        ShopRole role = findRole(uuid, staff);
        if (role.isOwnerRole()) {
            throw new ForbiddenException("role.owner.locked");
        }
        if (roleRepository.existsByShop_RecordIdAndNameIgnoreCaseAndDeletedFalseAndRecordIdNot(
                staff.getShop().getRecordId(), req.getName().trim(), role.getRecordId())) {
            throw new DuplicateResourceException("role.name.exists");
        }
        applyRole(role, req);
        role.setModifiedBy(user.getRecordId());
        return toRoleDetail(roleRepository.save(role));
    }

    @Transactional
    public void updateRoleStatus(SystemUser user, String uuid, boolean active) {
        ShopStaff staff = access.require(user, ShopActivity.ROLE_STATUS);
        ShopRole role = findRole(uuid, staff);
        if (role.isOwnerRole()) {
            throw new ForbiddenException("role.owner.locked");
        }
        role.setActive(active);
        role.setModifiedBy(user.getRecordId());
        roleRepository.save(role);
    }

    private void applyRole(ShopRole role, ShopPartnerDTO.RoleSaveOrUpdate req) {
        role.setName(req.getName().trim());
        role.setDescription(req.getDescription());
        role.getActivities().clear();
        if (req.getActivities() != null) {
            role.getActivities().addAll(req.getActivities());
            // MANAGE implies VIEW: store it explicitly so the role reads naturally
            req.getActivities().stream()
                    .filter(ShopActivity::isWrite)
                    .forEach(a -> role.getActivities().add(a.viewActivity()));
        }
    }

    private ShopRole findRole(String uuid, ShopStaff staff) {
        return roleRepository.findByUuidAndShop_RecordIdAndDeletedFalse(uuid, staff.getShop().getRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private ShopPartnerDTO.RoleDetail toRoleDetail(ShopRole role) {
        ShopPartnerDTO.RoleDetail d = new ShopPartnerDTO.RoleDetail();
        d.setUuid(role.getUuid());
        d.setName(role.getName());
        d.setDescription(role.getDescription());
        d.setOwnerRole(role.isOwnerRole());
        d.setActivities(role.isOwnerRole()
                ? java.util.Arrays.stream(ShopActivity.values()).filter(a -> !a.isLegacy()).toList()
                : role.getActivities().stream().sorted().toList());
        d.setActive(role.isActive());
        return d;
    }

    // =====================================================================
    // Staff logins
    // =====================================================================

    @Transactional(readOnly = true)
    public DataTableResponsePacket<ShopPartnerDTO.StaffDetail> listStaff(
            SystemUser user, ShopPartnerDTO.ListOrSearchData req) {

        ShopStaff me = access.require(user, ShopActivity.STAFF_VIEW);
        Long shopId = me.getShop().getRecordId();

        Specification<ShopStaff> spec = (root, query, cb) -> {
            Join<ShopStaff, SystemUser> u = root.join("user");
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("shop").get("recordId"), shopId));
            p.add(cb.isFalse(root.get("deleted")));
            String pattern = SearchUtils.containsPattern(req.getSearch());
            if (pattern != null) {
                char esc = SearchUtils.LIKE_ESCAPE;
                p.add(cb.or(
                        cb.like(cb.lower(u.get("fullName")), pattern, esc),
                        cb.like(cb.lower(u.get("email")), pattern, esc),
                        cb.like(cb.lower(u.get("mobile")), pattern, esc)));
            }
            if (req.getRoleUuid() != null && !req.getRoleUuid().isBlank()) {
                p.add(cb.equal(root.get("role").get("uuid"), req.getRoleUuid()));
            }
            if (req.getFilters() != null && req.getFilters().getActive() != null) {
                p.add(cb.equal(root.get("active"), req.getFilters().getActive()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };

        return PaginationUtils.createResponse(
                staffRepository.findAll(spec, page(req, Set.of("createdAt"), "createdAt"))
                        .map(this::toStaffDetail));
    }

    @Transactional(readOnly = true)
    public ShopPartnerDTO.StaffDetail getStaff(SystemUser user, String uuid) {
        ShopStaff me = access.require(user, ShopActivity.STAFF_VIEW);
        return toStaffDetail(findStaff(uuid, me));
    }

    @Transactional
    public ShopPartnerDTO.StaffDetail saveStaff(SystemUser user, ShopPartnerDTO.StaffSaveOrUpdate req) {
        ShopStaff me = access.require(user, ShopActivity.STAFF_ADD);
        ShopRole role = assignableRole(req.getRoleUuid(), me);

        SystemUser login = loginHelper.create(req.getFullName(), req.getEmail(), req.getMobile(),
                req.getPassword(), UserTypeEnum.SHOP_USER, user.getRecordId());

        ShopStaff staff = new ShopStaff();
        staff.setShop(me.getShop());
        staff.setUser(login);
        staff.setRole(role);
        staff.setCreatedBy(user.getRecordId());
        staff.setModifiedBy(user.getRecordId());
        return toStaffDetail(staffRepository.save(staff));
    }

    @Transactional
    public ShopPartnerDTO.StaffDetail updateStaff(SystemUser user, String uuid, ShopPartnerDTO.StaffSaveOrUpdate req) {
        ShopStaff me = access.require(user, ShopActivity.STAFF_EDIT);
        ShopStaff staff = findStaff(uuid, me);

        if (staff.isOwner()) {
            // only the owner may edit the owner login; its role never changes
            if (!me.isOwner()) {
                throw new ForbiddenException("staff.owner.locked");
            }
        } else {
            staff.setRole(assignableRole(req.getRoleUuid(), me));
        }

        loginHelper.update(staff.getUser(), req.getFullName(), req.getEmail(), req.getMobile(),
                req.getPassword(), user.getRecordId());
        staff.setModifiedBy(user.getRecordId());
        return toStaffDetail(staffRepository.save(staff));
    }

    @Transactional
    public void updateStaffStatus(SystemUser user, String uuid, boolean active) {
        ShopStaff me = access.require(user, ShopActivity.STAFF_STATUS);
        ShopStaff staff = findStaff(uuid, me);
        if (staff.isOwner()) {
            throw new ForbiddenException("staff.owner.locked");
        }
        if (staff.getRecordId().equals(me.getRecordId())) {
            throw new ForbiddenException("staff.self.status");
        }
        staff.setActive(active);
        staff.setModifiedBy(user.getRecordId());
        staffRepository.save(staff);
        loginHelper.setActive(staff.getUser(), active, user.getRecordId());
    }

    private ShopRole assignableRole(String roleUuid, ShopStaff me) {
        ShopRole role = findRole(roleUuid, me);
        if (role.isOwnerRole()) {
            throw new BadRequestException("role.owner.locked");
        }
        if (!role.isActive()) {
            throw new BadRequestException("role.inactive");
        }
        return role;
    }

    private ShopStaff findStaff(String uuid, ShopStaff me) {
        return staffRepository.findByUuidAndShop_RecordIdAndDeletedFalse(uuid, me.getShop().getRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private ShopPartnerDTO.StaffDetail toStaffDetail(ShopStaff staff) {
        SystemUser u = staff.getUser();
        ShopPartnerDTO.StaffDetail d = new ShopPartnerDTO.StaffDetail();
        d.setUuid(staff.getUuid());
        d.setFullName(u.getFullName());
        d.setEmail(u.getEmail());
        d.setMobile(u.getMobile());
        d.setRoleUuid(staff.getRole().getUuid());
        d.setRoleName(staff.getRole().getName());
        d.setOwner(staff.isOwner());
        d.setActive(staff.isActive());
        d.setLastLogin(u.getLastLogin());
        return d;
    }

    // =====================================================================
    // Admin: owner login of a shop
    // =====================================================================

    /** Name of the per-shop role with every activity, assignable to the shop's admin. */
    public static final String ADMIN_ROLE_NAME = "Shop Admin";

    @Transactional(readOnly = true)
    public ShopPartnerDTO.StaffDetail getOwner(String shopUuid) {
        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(shopUuid)
                .orElseThrow(() -> new ResourceNotFoundException("shop.notfound"));
        SystemUser owner = shop.getOwner();
        return owner == null || owner.isDeleted() ? null : ownerDetail(owner);
    }

    /** Creates the owner login (first time) or updates its details / password. */
    @Transactional
    public ShopPartnerDTO.StaffDetail saveOwner(String shopUuid, ShopPartnerDTO.OwnerSave req, SystemUser admin) {
        return saveOwner(shopUuid, req, admin.getRecordId());
    }

    /**
     * actorId = admin creating the owner, or null for public self-registration.
     * New owners are SHOP_OWNER logins linked through shop.owner (one owner can own many shops).
     * Owners created before that (SHOP_USER + owner staff row) are still updated in place.
     */
    @Transactional
    public ShopPartnerDTO.StaffDetail saveOwner(String shopUuid, ShopPartnerDTO.OwnerSave req, Long actorId) {
        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(shopUuid)
                .orElseThrow(() -> new ResourceNotFoundException("shop.notfound"));

        SystemUser owner = shop.getOwner();
        if (owner == null || owner.isDeleted()) {
            owner = loginHelper.create(req.getFullName(), req.getEmail(), req.getMobile(),
                    req.getPassword(), UserTypeEnum.SHOP_OWNER, actorId);
        } else {
            loginHelper.update(owner, req.getFullName(), req.getEmail(), req.getMobile(),
                    req.getPassword(), actorId);
        }
        shop.setOwner(owner);
        shop.setModifiedBy(actorId);
        shopRepository.save(shop);
        ensureAdminRole(shop, actorId);
        return ownerDetail(owner);
    }

    /** Admin: give an existing owner (who already owns other shops) this shop too. */
    @Transactional
    public ShopPartnerDTO.StaffDetail assignExistingOwner(String shopUuid, String ownerUuid, Long actorId) {
        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(shopUuid)
                .orElseThrow(() -> new ResourceNotFoundException("shop.notfound"));
        SystemUser owner = userRepository.findByUuidAndDeletedFalse(ownerUuid)
                .filter(u -> u.getUserType() == UserTypeEnum.SHOP_OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("owner.notfound"));
        shop.setOwner(owner);
        shop.setModifiedBy(actorId);
        shopRepository.save(shop);
        ensureAdminRole(shop, actorId);
        return ownerDetail(owner);
    }

    /** Owners for the "existing owner" dropdown. */
    @Transactional(readOnly = true)
    public List<com.bt.fretchain.bean.KeyValueDto> ownerKeyValue() {
        return userRepository.findByUserTypeAndDeletedFalseOrderByFullNameAsc(UserTypeEnum.SHOP_OWNER).stream()
                .map(u -> new com.bt.fretchain.bean.KeyValueDto(u.getUuid(), u.getFullName() + " (" + u.getEmail() + ")"))
                .toList();
    }

    /** Every shop gets an editable "Shop Admin" role with all activities. */
    private void ensureAdminRole(ShopMaster shop, Long actorId) {
        boolean exists = roleRepository.existsByShop_RecordIdAndNameIgnoreCaseAndDeletedFalse(shop.getRecordId(), ADMIN_ROLE_NAME);
        if (!exists) {
            ShopRole r = new ShopRole();
            r.setShop(shop);
            r.setName(ADMIN_ROLE_NAME);
            r.setDescription("Full access to this shop");
            java.util.Arrays.stream(ShopActivity.values()).filter(a -> !a.isLegacy()).forEach(r.getActivities()::add);
            r.setCreatedBy(actorId);
            r.setModifiedBy(actorId);
            roleRepository.save(r);
        }
    }

    private ShopPartnerDTO.StaffDetail ownerDetail(SystemUser u) {
        ShopPartnerDTO.StaffDetail d = new ShopPartnerDTO.StaffDetail();
        d.setUuid(u.getUuid());
        d.setFullName(u.getFullName());
        d.setEmail(u.getEmail());
        d.setMobile(u.getMobile());
        d.setOwner(true);
        d.setRoleName("Owner");
        d.setActive(u.isActive());
        d.setLastLogin(u.getLastLogin());
        return d;
    }

    // =====================================================================

    private static Pageable page(ShopPartnerDTO.ListOrSearchData req, Set<String> allowed, String defaultField) {
        String field = defaultField;
        Sort.Direction direction = "name".equals(defaultField) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (req.getSort() != null && req.getSort().getField() != null && allowed.contains(req.getSort().getField())) {
            field = req.getSort().getField();
            direction = "desc".equalsIgnoreCase(req.getSort().getDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        }
        int pageNumber = Math.max(0, req.getPageNumber() == null ? 0 : req.getPageNumber());
        int pageSize = req.getPageSize() == null ? 10 : Math.max(1, Math.min(100, req.getPageSize()));
        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, field));
    }
}
