package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.PlatformStaffDTO;
import com.bt.fretchain.primary.pojo.PlatformRole;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.PlatformActivity;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.PlatformRoleRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Admin-created roles (Accountant, Order Manager...) and admin staff logins (ADMIN_STAFF).
 * Access to these endpoints is decided by PanelAuthorizationInterceptor (ADMIN_ROLE_* / ADMIN_STAFF_*).
 */
@Service
@RequiredArgsConstructor
public class PlatformStaffService {

    private final PlatformRoleRepository roleRepository;
    private final SystemUserRepository userRepository;
    private final StoreLoginHelper loginHelper;

    // ---------------------------------------------------------------- roles

    public List<PlatformStaffDTO.ActivityOption> activityCatalog() {
        List<PlatformStaffDTO.ActivityOption> list = new ArrayList<>();
        for (PlatformActivity a : PlatformActivity.values()) {
            PlatformStaffDTO.ActivityOption o = new PlatformStaffDTO.ActivityOption();
            o.setCode(a);
            o.setGroup(a.getMenu().name());
            o.setAction(a.getAction().name());
            list.add(o);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public DataTableResponsePacket<PlatformStaffDTO.RoleDetail> listRoles(PlatformStaffDTO.ListOrSearchData req) {
        Specification<PlatformRole> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
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
                roleRepository.findAll(spec, page(req, Set.of("name", "createdAt"), "name", true)).map(this::toRoleDetail));
    }

    @Transactional(readOnly = true)
    public List<KeyValueDto> roleKeyValue() {
        PlatformStaffDTO.ListOrSearchData req = new PlatformStaffDTO.ListOrSearchData();
        req.setPageSize(100);
        PlatformStaffDTO.FilterData f = new PlatformStaffDTO.FilterData();
        f.setActive(true);
        req.setFilters(f);
        return listRoles(req).getData().stream().map(r -> new KeyValueDto(r.getUuid(), r.getName())).toList();
    }

    @Transactional(readOnly = true)
    public PlatformStaffDTO.RoleDetail getRole(String uuid) {
        return toRoleDetail(findRole(uuid));
    }

    @Transactional
    public PlatformStaffDTO.RoleDetail saveRole(PlatformStaffDTO.RoleSaveOrUpdate req, SystemUser actor) {
        if (roleRepository.existsByNameIgnoreCaseAndDeletedFalse(req.getName().trim())) {
            throw new DuplicateResourceException("role.name.exists");
        }
        PlatformRole role = new PlatformRole();
        apply(role, req);
        role.setCreatedBy(actor.getRecordId());
        role.setModifiedBy(actor.getRecordId());
        return toRoleDetail(roleRepository.save(role));
    }

    @Transactional
    public PlatformStaffDTO.RoleDetail updateRole(String uuid, PlatformStaffDTO.RoleSaveOrUpdate req, SystemUser actor) {
        PlatformRole role = findRole(uuid);
        guardOwnRole(role, actor);
        if (roleRepository.existsByNameIgnoreCaseAndDeletedFalseAndRecordIdNot(req.getName().trim(), role.getRecordId())) {
            throw new DuplicateResourceException("role.name.exists");
        }
        apply(role, req);
        role.setModifiedBy(actor.getRecordId());
        return toRoleDetail(roleRepository.save(role));
    }

    @Transactional
    public void updateRoleStatus(String uuid, boolean active, SystemUser actor) {
        PlatformRole role = findRole(uuid);
        guardOwnRole(role, actor);
        role.setActive(active);
        role.setModifiedBy(actor.getRecordId());
        roleRepository.save(role);
    }

    /** Staff may not change their own role (no self-promotion). */
    private void guardOwnRole(PlatformRole role, SystemUser actor) {
        if (actor.getPlatformRole() != null && actor.getPlatformRole().getRecordId().equals(role.getRecordId())) {
            throw new ForbiddenException("role.own.locked");
        }
    }

    private void apply(PlatformRole role, PlatformStaffDTO.RoleSaveOrUpdate req) {
        role.setName(req.getName().trim());
        role.setDescription(req.getDescription());
        role.getActivities().clear();
        if (req.getActivities() != null) {
            role.getActivities().addAll(req.getActivities());
            req.getActivities().forEach(a ->
                    role.getActivities().add(PlatformActivity.of(a.getMenu(), PlatformActivity.Action.VIEW)));
        }
    }

    private PlatformRole findRole(String uuid) {
        return roleRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private PlatformStaffDTO.RoleDetail toRoleDetail(PlatformRole role) {
        PlatformStaffDTO.RoleDetail d = new PlatformStaffDTO.RoleDetail();
        d.setUuid(role.getUuid());
        d.setName(role.getName());
        d.setDescription(role.getDescription());
        d.setActivities(role.getActivities().stream().sorted().toList());
        d.setActive(role.isActive());
        return d;
    }

    // ---------------------------------------------------------------- staff

    @Transactional(readOnly = true)
    public DataTableResponsePacket<PlatformStaffDTO.StaffDetail> listStaff(PlatformStaffDTO.ListOrSearchData req) {
        Specification<SystemUser> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("userType"), UserTypeEnum.ADMIN_STAFF));
            p.add(cb.isFalse(root.get("deleted")));
            String pattern = SearchUtils.containsPattern(req.getSearch());
            if (pattern != null) {
                char esc = SearchUtils.LIKE_ESCAPE;
                p.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, esc),
                        cb.like(cb.lower(root.get("email")), pattern, esc),
                        cb.like(cb.lower(root.get("mobile")), pattern, esc)));
            }
            if (req.getRoleUuid() != null && !req.getRoleUuid().isBlank()) {
                p.add(cb.equal(root.get("platformRole").get("uuid"), req.getRoleUuid()));
            }
            if (req.getFilters() != null && req.getFilters().getActive() != null) {
                p.add(cb.equal(root.get("active"), req.getFilters().getActive()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PaginationUtils.createResponse(
                userRepository.findAll(spec, page(req, Set.of("fullName", "createdAt"), "createdAt", false)).map(this::toStaffDetail));
    }

    @Transactional(readOnly = true)
    public PlatformStaffDTO.StaffDetail getStaff(String uuid) {
        return toStaffDetail(findStaff(uuid));
    }

    @Transactional
    public PlatformStaffDTO.StaffDetail saveStaff(PlatformStaffDTO.StaffSaveOrUpdate req, SystemUser actor) {
        PlatformRole role = activeRole(req.getRoleUuid());
        SystemUser user = loginHelper.create(req.getFullName(), req.getEmail(), req.getMobile(),
                req.getPassword(), UserTypeEnum.ADMIN_STAFF, actor.getRecordId());
        user.setPlatformRole(role);
        return toStaffDetail(userRepository.save(user));
    }

    @Transactional
    public PlatformStaffDTO.StaffDetail updateStaff(String uuid, PlatformStaffDTO.StaffSaveOrUpdate req, SystemUser actor) {
        SystemUser user = findStaff(uuid);
        if (user.getRecordId().equals(actor.getRecordId()) && !actor.getPlatformRole().getUuid().equals(req.getRoleUuid())) {
            throw new ForbiddenException("role.own.locked");   // no self-promotion
        }
        user.setPlatformRole(activeRole(req.getRoleUuid()));
        loginHelper.update(user, req.getFullName(), req.getEmail(), req.getMobile(), req.getPassword(), actor.getRecordId());
        return toStaffDetail(user);
    }

    @Transactional
    public void updateStaffStatus(String uuid, boolean active, SystemUser actor) {
        SystemUser user = findStaff(uuid);
        if (user.getRecordId().equals(actor.getRecordId())) {
            throw new ForbiddenException("staff.self.status");
        }
        loginHelper.setActive(user, active, actor.getRecordId());
    }

    private PlatformRole activeRole(String uuid) {
        PlatformRole role = findRole(uuid);
        if (!role.isActive()) {
            throw new BadRequestException("role.inactive");
        }
        return role;
    }

    private SystemUser findStaff(String uuid) {
        return userRepository.findByUuidAndDeletedFalse(uuid)
                .filter(u -> u.getUserType() == UserTypeEnum.ADMIN_STAFF)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private PlatformStaffDTO.StaffDetail toStaffDetail(SystemUser u) {
        PlatformStaffDTO.StaffDetail d = new PlatformStaffDTO.StaffDetail();
        d.setUuid(u.getUuid());
        d.setFullName(u.getFullName());
        d.setEmail(u.getEmail());
        d.setMobile(u.getMobile());
        if (u.getPlatformRole() != null) {
            d.setRoleUuid(u.getPlatformRole().getUuid());
            d.setRoleName(u.getPlatformRole().getName());
        }
        d.setActive(u.isActive());
        d.setLastLogin(u.getLastLogin());
        return d;
    }

    private static Pageable page(PlatformStaffDTO.ListOrSearchData req, Set<String> allowed, String def, boolean asc) {
        String field = def;
        Sort.Direction dir = asc ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (req.getSort() != null && req.getSort().getField() != null && allowed.contains(req.getSort().getField())) {
            field = req.getSort().getField();
            dir = "desc".equalsIgnoreCase(req.getSort().getDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        }
        int n = Math.max(0, req.getPageNumber() == null ? 0 : req.getPageNumber());
        int size = req.getPageSize() == null ? 10 : Math.max(1, Math.min(100, req.getPageSize()));
        return PageRequest.of(n, size, Sort.by(dir, field));
    }
}
