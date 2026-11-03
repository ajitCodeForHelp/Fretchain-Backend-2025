package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantMasterDTO;
import com.bt.fretchain.primary.dto.RestaurantPartnerDTO;
import com.bt.fretchain.primary.mapper.RestaurantMasterMapper;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.RestaurantActivity;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.RestaurantMasterRepository;
import com.bt.fretchain.primary.repository.RestaurantRoleRepository;
import com.bt.fretchain.primary.repository.RestaurantStaffRepository;
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
 * Restaurant self-service: own profile, roles (activity mapping) and staff logins,
 * plus the admin action that creates the restaurant owner login.
 */
@Service
@RequiredArgsConstructor
public class RestaurantPartnerService {

    private static final String OWNER_ROLE_NAME = "Owner";

    private final RestaurantAccessService access;
    private final RestaurantMasterRepository restaurantRepository;
    private final RestaurantRoleRepository roleRepository;
    private final RestaurantStaffRepository staffRepository;
    private final RestaurantMasterMapper restaurantMapper;
    private final StoreLoginHelper loginHelper;

    // =====================================================================
    // Profile
    // =====================================================================

    @Transactional(readOnly = true)
    public RestaurantPartnerDTO.Session session(SystemUser user) {
        return access.session(access.currentStaff(user));
    }

    @Transactional(readOnly = true)
    public RestaurantMasterDTO.Detail getProfile(SystemUser user) {
        RestaurantStaff staff = access.require(user, RestaurantActivity.PROFILE_VIEW);
        return restaurantMapper.toDetailDto(staff.getRestaurant());
    }

    @Transactional
    public RestaurantMasterDTO.Detail updateProfile(SystemUser user, RestaurantPartnerDTO.ProfileUpdate req) {
        RestaurantStaff staff = access.require(user, RestaurantActivity.PROFILE_EDIT);

        if ((req.getOpeningTime() == null) != (req.getClosingTime() == null)
                || (req.getOpeningTime() != null && req.getOpeningTime().equals(req.getClosingTime()))) {
            throw new BadRequestException("restaurant.timing.invalid");
        }

        RestaurantMaster restaurant = staff.getRestaurant();
        restaurant.setNameEn(req.getNameEn().trim());
        restaurant.setNameFr(req.getNameFr());
        restaurant.setDescriptionEn(req.getDescriptionEn());
        restaurant.setDescriptionFr(req.getDescriptionFr());
        restaurant.setCountryCode(req.getCountryCode());
        restaurant.setMobile(req.getMobile());
        restaurant.setEmail(req.getEmail());
        restaurant.setLogoUrl(req.getLogoUrl());
        restaurant.setBannerUrl(req.getBannerUrl());
        restaurant.setAddress(req.getAddress());
        restaurant.setCity(req.getCity());
        restaurant.setState(req.getState());
        restaurant.setCountry(req.getCountry());
        restaurant.setPostalCode(req.getPostalCode());
        restaurant.setLatitude(req.getLatitude());
        restaurant.setLongitude(req.getLongitude());
        restaurant.setDeliveryRadiusKm(req.getDeliveryRadiusKm());
        restaurant.setOpeningTime(req.getOpeningTime());
        restaurant.setClosingTime(req.getClosingTime());
        restaurant.setAcceptingOrders(req.getAcceptingOrders() == null || req.getAcceptingOrders());
        restaurant.setAvgPreparationMinutes(req.getAvgPreparationMinutes());
        restaurant.setMinOrderAmount(req.getMinOrderAmount());
        restaurant.setDeliveryFee(req.getDeliveryFee());
        restaurant.setFoodType(req.getFoodType());
        restaurant.setCostForTwo(req.getCostForTwo());
        restaurant.setModifiedBy(user.getRecordId());

        return restaurantMapper.toDetailDto(restaurantRepository.save(restaurant));
    }

    // =====================================================================
    // Roles (activity mapping)
    // =====================================================================

    public List<RestaurantPartnerDTO.ActivityOption> activityCatalog() {
        List<RestaurantPartnerDTO.ActivityOption> list = new ArrayList<>();
        for (RestaurantActivity a : RestaurantActivity.values()) {
            if (a.isLegacy()) {
                continue;
            }
            RestaurantPartnerDTO.ActivityOption o = new RestaurantPartnerDTO.ActivityOption();
            o.setCode(a);
            o.setGroup(a.getGroup());
            o.setWrite(a.isWrite());
            o.setAction(a.getAction().name());
            list.add(o);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public DataTableResponsePacket<RestaurantPartnerDTO.RoleDetail> listRoles(
            SystemUser user, RestaurantPartnerDTO.ListOrSearchData req) {

        RestaurantStaff staff = access.require(user, RestaurantActivity.ROLE_VIEW);
        Long restaurantId = staff.getRestaurant().getRecordId();

        Specification<RestaurantRole> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("restaurant").get("recordId"), restaurantId));
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
    public RestaurantPartnerDTO.RoleDetail getRole(SystemUser user, String uuid) {
        RestaurantStaff staff = access.require(user, RestaurantActivity.ROLE_VIEW);
        return toRoleDetail(findRole(uuid, staff));
    }

    @Transactional
    public RestaurantPartnerDTO.RoleDetail saveRole(SystemUser user, RestaurantPartnerDTO.RoleSaveOrUpdate req) {
        RestaurantStaff staff = access.require(user, RestaurantActivity.ROLE_ADD);
        Long restaurantId = staff.getRestaurant().getRecordId();

        if (roleRepository.existsByRestaurant_RecordIdAndNameIgnoreCaseAndDeletedFalse(restaurantId, req.getName().trim())) {
            throw new DuplicateResourceException("role.name.exists");
        }

        RestaurantRole role = new RestaurantRole();
        role.setRestaurant(staff.getRestaurant());
        applyRole(role, req);
        role.setCreatedBy(user.getRecordId());
        role.setModifiedBy(user.getRecordId());
        return toRoleDetail(roleRepository.save(role));
    }

    @Transactional
    public RestaurantPartnerDTO.RoleDetail updateRole(SystemUser user, String uuid, RestaurantPartnerDTO.RoleSaveOrUpdate req) {
        RestaurantStaff staff = access.require(user, RestaurantActivity.ROLE_EDIT);
        RestaurantRole role = findRole(uuid, staff);
        if (role.isOwnerRole()) {
            throw new ForbiddenException("role.owner.locked");
        }
        if (roleRepository.existsByRestaurant_RecordIdAndNameIgnoreCaseAndDeletedFalseAndRecordIdNot(
                staff.getRestaurant().getRecordId(), req.getName().trim(), role.getRecordId())) {
            throw new DuplicateResourceException("role.name.exists");
        }
        applyRole(role, req);
        role.setModifiedBy(user.getRecordId());
        return toRoleDetail(roleRepository.save(role));
    }

    @Transactional
    public void updateRoleStatus(SystemUser user, String uuid, boolean active) {
        RestaurantStaff staff = access.require(user, RestaurantActivity.ROLE_STATUS);
        RestaurantRole role = findRole(uuid, staff);
        if (role.isOwnerRole()) {
            throw new ForbiddenException("role.owner.locked");
        }
        role.setActive(active);
        role.setModifiedBy(user.getRecordId());
        roleRepository.save(role);
    }

    private void applyRole(RestaurantRole role, RestaurantPartnerDTO.RoleSaveOrUpdate req) {
        role.setName(req.getName().trim());
        role.setDescription(req.getDescription());
        role.getActivities().clear();
        if (req.getActivities() != null) {
            role.getActivities().addAll(req.getActivities());
            // MANAGE implies VIEW: store it explicitly so the role reads naturally
            req.getActivities().stream()
                    .filter(RestaurantActivity::isWrite)
                    .forEach(a -> role.getActivities().add(a.viewActivity()));
        }
    }

    private RestaurantRole findRole(String uuid, RestaurantStaff staff) {
        return roleRepository.findByUuidAndRestaurant_RecordIdAndDeletedFalse(uuid, staff.getRestaurant().getRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private RestaurantPartnerDTO.RoleDetail toRoleDetail(RestaurantRole role) {
        RestaurantPartnerDTO.RoleDetail d = new RestaurantPartnerDTO.RoleDetail();
        d.setUuid(role.getUuid());
        d.setName(role.getName());
        d.setDescription(role.getDescription());
        d.setOwnerRole(role.isOwnerRole());
        d.setActivities(role.isOwnerRole()
                ? java.util.Arrays.stream(RestaurantActivity.values()).filter(a -> !a.isLegacy()).toList()
                : role.getActivities().stream().sorted().toList());
        d.setActive(role.isActive());
        return d;
    }

    // =====================================================================
    // Staff logins
    // =====================================================================

    @Transactional(readOnly = true)
    public DataTableResponsePacket<RestaurantPartnerDTO.StaffDetail> listStaff(
            SystemUser user, RestaurantPartnerDTO.ListOrSearchData req) {

        RestaurantStaff me = access.require(user, RestaurantActivity.STAFF_VIEW);
        Long restaurantId = me.getRestaurant().getRecordId();

        Specification<RestaurantStaff> spec = (root, query, cb) -> {
            Join<RestaurantStaff, SystemUser> u = root.join("user");
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("restaurant").get("recordId"), restaurantId));
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
    public RestaurantPartnerDTO.StaffDetail getStaff(SystemUser user, String uuid) {
        RestaurantStaff me = access.require(user, RestaurantActivity.STAFF_VIEW);
        return toStaffDetail(findStaff(uuid, me));
    }

    @Transactional
    public RestaurantPartnerDTO.StaffDetail saveStaff(SystemUser user, RestaurantPartnerDTO.StaffSaveOrUpdate req) {
        RestaurantStaff me = access.require(user, RestaurantActivity.STAFF_ADD);
        RestaurantRole role = assignableRole(req.getRoleUuid(), me);

        SystemUser login = loginHelper.create(req.getFullName(), req.getEmail(), req.getMobile(),
                req.getPassword(), UserTypeEnum.RESTAURANT_USER, user.getRecordId());

        RestaurantStaff staff = new RestaurantStaff();
        staff.setRestaurant(me.getRestaurant());
        staff.setUser(login);
        staff.setRole(role);
        staff.setCreatedBy(user.getRecordId());
        staff.setModifiedBy(user.getRecordId());
        return toStaffDetail(staffRepository.save(staff));
    }

    @Transactional
    public RestaurantPartnerDTO.StaffDetail updateStaff(SystemUser user, String uuid, RestaurantPartnerDTO.StaffSaveOrUpdate req) {
        RestaurantStaff me = access.require(user, RestaurantActivity.STAFF_EDIT);
        RestaurantStaff staff = findStaff(uuid, me);

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
        RestaurantStaff me = access.require(user, RestaurantActivity.STAFF_STATUS);
        RestaurantStaff staff = findStaff(uuid, me);
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

    private RestaurantRole assignableRole(String roleUuid, RestaurantStaff me) {
        RestaurantRole role = findRole(roleUuid, me);
        if (role.isOwnerRole()) {
            throw new BadRequestException("role.owner.locked");
        }
        if (!role.isActive()) {
            throw new BadRequestException("role.inactive");
        }
        return role;
    }

    private RestaurantStaff findStaff(String uuid, RestaurantStaff me) {
        return staffRepository.findByUuidAndRestaurant_RecordIdAndDeletedFalse(uuid, me.getRestaurant().getRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private RestaurantPartnerDTO.StaffDetail toStaffDetail(RestaurantStaff staff) {
        SystemUser u = staff.getUser();
        RestaurantPartnerDTO.StaffDetail d = new RestaurantPartnerDTO.StaffDetail();
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
    // Admin: owner login of a restaurant
    // =====================================================================

    @Transactional(readOnly = true)
    public RestaurantPartnerDTO.StaffDetail getOwner(String restaurantUuid) {
        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(restaurantUuid)
                .orElseThrow(() -> new ResourceNotFoundException("restaurant.notfound"));
        return staffRepository.findFirstByRestaurant_RecordIdAndOwnerTrueAndDeletedFalse(restaurant.getRecordId())
                .map(this::toStaffDetail)
                .orElse(null);
    }

    /** Creates the owner login (first time) or updates its details / password. */
    @Transactional
    public RestaurantPartnerDTO.StaffDetail saveOwner(String restaurantUuid, RestaurantPartnerDTO.OwnerSave req, SystemUser admin) {
        return saveOwner(restaurantUuid, req, admin.getRecordId());
    }

    /** actorId = admin creating the owner, or null for public self-registration. */
    @Transactional
    public RestaurantPartnerDTO.StaffDetail saveOwner(String restaurantUuid, RestaurantPartnerDTO.OwnerSave req, Long actorId) {
        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(restaurantUuid)
                .orElseThrow(() -> new ResourceNotFoundException("restaurant.notfound"));

        RestaurantRole ownerRole = roleRepository.findFirstByRestaurant_RecordIdAndOwnerRoleTrueAndDeletedFalse(restaurant.getRecordId())
                .orElseGet(() -> {
                    RestaurantRole r = new RestaurantRole();
                    r.setRestaurant(restaurant);
                    r.setName(OWNER_ROLE_NAME);
                    r.setDescription("Full access to the restaurant");
                    r.setOwnerRole(true);
                    r.getActivities().addAll(EnumSet.allOf(RestaurantActivity.class));
                    r.setCreatedBy(actorId);
                    r.setModifiedBy(actorId);
                    return roleRepository.save(r);
                });

        RestaurantStaff owner = staffRepository.findFirstByRestaurant_RecordIdAndOwnerTrueAndDeletedFalse(restaurant.getRecordId())
                .orElse(null);

        if (owner == null) {
            SystemUser login = loginHelper.create(req.getFullName(), req.getEmail(), req.getMobile(),
                    req.getPassword(), UserTypeEnum.RESTAURANT_USER, actorId);
            owner = new RestaurantStaff();
            owner.setRestaurant(restaurant);
            owner.setUser(login);
            owner.setRole(ownerRole);
            owner.setOwner(true);
            owner.setCreatedBy(actorId);
        } else {
            loginHelper.update(owner.getUser(), req.getFullName(), req.getEmail(), req.getMobile(),
                    req.getPassword(), actorId);
        }
        owner.setModifiedBy(actorId);
        owner = staffRepository.save(owner);

        restaurant.setOwner(owner.getUser());
        restaurant.setModifiedBy(actorId);
        restaurantRepository.save(restaurant);

        return toStaffDetail(owner);
    }

    // =====================================================================

    private static Pageable page(RestaurantPartnerDTO.ListOrSearchData req, Set<String> allowed, String defaultField) {
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
