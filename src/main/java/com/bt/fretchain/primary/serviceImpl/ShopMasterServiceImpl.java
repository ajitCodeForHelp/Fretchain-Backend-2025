package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.mapper.ShopMasterMapper;
import com.bt.fretchain.primary.pojo.ShopCategory;
import com.bt.fretchain.primary.pojo.ShopGlobalCategory;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import com.bt.fretchain.primary.repository.ShopCategoryRepository;
import com.bt.fretchain.primary.repository.ShopGlobalCategoryRepository;
import com.bt.fretchain.primary.repository.ShopMasterRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.service.ShopMasterService;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopMasterServiceImpl implements ShopMasterService {

    private final ShopMasterRepository repository;
    private final ShopMasterMapper mapper;
    private final SystemUserRepository userRepository;
    private final ShopGlobalCategoryRepository globalCategoryRepository;
    private final ShopCategoryRepository shopCategoryRepository;

    @Override
    @Transactional
    public ShopMasterDTO.Detail save(
            ShopMasterDTO.SaveOrUpdate request,
            SystemUser user) {

        validateShopCode(request.getShopCode(), null);
        validateTimings(request);

        ShopMaster entity = mapper.toEntity(request);
        applyDefaults(entity, ShopStatus.PENDING);

        entity.setGlobalCategories(resolveGlobalCategories(
                request.getGlobalCategoryUuids()));

        if (request.getOwnerUuid() != null
                && !request.getOwnerUuid().isBlank()) {
            entity.setOwner(userRepository.findByUuidAndDeletedFalse(
                    request.getOwnerUuid()
            ).orElseThrow(() ->
                    new ResourceNotFoundException("user.notfound")));
        }

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ShopMasterDTO.Detail update(
            String uuid,
            ShopMasterDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopMaster entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        validateShopCode(request.getShopCode(), entity.getRecordId());
        validateTimings(request);

        ShopStatus previousStatus = entity.getStatus();

        mapper.updateEntityFromDto(request, entity);
        applyDefaults(entity, previousStatus);

        Set<ShopGlobalCategory> globalCategories = resolveGlobalCategories(
                request.getGlobalCategoryUuids());

        // Replace contents (keeps the Hibernate-managed collection instance)
        entity.getGlobalCategories().clear();
        entity.getGlobalCategories().addAll(globalCategories);

        unlinkRemovedGlobalCategories(entity, globalCategories, user);

        if (request.getOwnerUuid() != null
                && !request.getOwnerUuid().isBlank()) {
            entity.setOwner(userRepository.findByUuidAndDeletedFalse(
                    request.getOwnerUuid()
            ).orElseThrow(() ->
                    new ResourceNotFoundException("user.notfound")));
        }

        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public ShopMasterDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    @Transactional
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        ShopMaster entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(Boolean.TRUE.equals(active));
        entity.setModifiedBy(user.getRecordId());

        repository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String lang) {
        return repository.findByActiveTrueAndDeletedFalseOrderByNameEnAsc()
                .stream()
                .map(shop -> new KeyValueDto(
                        shop.getUuid(),
                        "fr".equalsIgnoreCase(lang) && shop.getNameFr() != null
                                ? shop.getNameFr()
                                : shop.getNameEn()
                ))
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** All requested global categories must exist and be active. */
    private Set<ShopGlobalCategory> resolveGlobalCategories(List<String> uuids) {

        Set<String> distinct = uuids == null ? Set.of() : uuids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (distinct.isEmpty()) {
            throw new BadRequestException("shop.global.category.required");
        }

        List<ShopGlobalCategory> found =
                globalCategoryRepository.findByUuidInAndDeletedFalse(distinct);

        if (found.size() != distinct.size()) {
            throw new ResourceNotFoundException("shop.global.category.notfound");
        }

        for (ShopGlobalCategory category : found) {
            if (!category.isActive()) {
                throw new BadRequestException("shop.global.category.invalid");
            }
        }

        return new LinkedHashSet<>(found);
    }

    /**
     * If a global category is removed from the store, shop sections that pointed
     * to it lose the link (they still exist, just unmapped).
     */
    private void unlinkRemovedGlobalCategories(
            ShopMaster shop, Set<ShopGlobalCategory> current, SystemUser user) {

        Set<Long> currentIds = current.stream()
                .map(ShopGlobalCategory::getRecordId)
                .collect(Collectors.toSet());

        for (ShopCategory section : shopCategoryRepository
                .findByShop_RecordIdAndGlobalCategoryIsNotNullAndDeletedFalse(
                        shop.getRecordId())) {

            if (!currentIds.contains(section.getGlobalCategory().getRecordId())) {
                section.setGlobalCategory(null);
                section.setModifiedBy(user.getRecordId());
                shopCategoryRepository.save(section);
            }
        }
    }

    private void applyDefaults(ShopMaster entity, ShopStatus statusIfMissing) {
        if (entity.getStatus() == null) {
            entity.setStatus(statusIfMissing == null ? ShopStatus.PENDING : statusIfMissing);
        }
        if (entity.getAcceptingOrders() == null) {
            entity.setAcceptingOrders(true);
        }
        if (entity.getCommissionPercentage() == null) {
            entity.setCommissionPercentage(BigDecimal.ZERO);
        }
    }

    private void validateTimings(ShopMasterDTO.SaveOrUpdate request) {
        boolean onlyOne = (request.getOpeningTime() == null)
                != (request.getClosingTime() == null);

        if (onlyOne) {
            throw new BadRequestException("shop.timing.invalid");
        }
        if (request.getOpeningTime() != null
                && request.getOpeningTime().equals(request.getClosingTime())) {
            throw new BadRequestException("shop.timing.invalid");
        }
    }

    private void validateShopCode(String code, Long currentId) {
        if (code == null || code.isBlank()) {
            return;
        }

        repository.findByShopCodeIgnoreCaseAndDeletedFalse(code.trim())
                .ifPresent(existing -> {
                    if (currentId == null
                            || !existing.getRecordId().equals(currentId)) {
                        throw new DuplicateResourceException(
                                "shop.code.exists");
                    }
                });
    }

    // ------------------------------------------------------------------
    // Admin list
    // ------------------------------------------------------------------

    @Override
    public DataTableResponsePacket<ShopMasterDTO.Detail> getListData(
            ShopMasterDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<ShopMaster> specification =
                createFilterOrSearch(request);

        Page<ShopMaster> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            ShopMasterDTO.ListOrSearchData request) {

        Set<String> allowedFields = Set.of(
                "createdAt", "nameEn", "nameFr",
                "shopCode", "mobile", "status",
                "city", "rating"
        );

        String field = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;

        if (request.getSort() != null
                && request.getSort().getField() != null
                && allowedFields.contains(request.getSort().getField())) {

            field = request.getSort().getField();

            if ("asc".equalsIgnoreCase(
                    request.getSort().getDirection())) {
                direction = Sort.Direction.ASC;
            }
        }

        int pageNumber = Math.max(
                0, request.getPageNumber() == null
                        ? 0 : request.getPageNumber());

        int pageSize = request.getPageSize() == null
                ? 10 : Math.max(1, Math.min(100, request.getPageSize()));

        return PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(direction, field)
        );
    }

    private Specification<ShopMaster> createFilterOrSearch(
            ShopMasterDTO.ListOrSearchData request) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            String pattern = SearchUtils.containsPattern(request.getSearch());
            if (pattern != null) {
                char esc = SearchUtils.LIKE_ESCAPE;
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), pattern, esc),
                        cb.like(cb.lower(root.get("nameFr")), pattern, esc),
                        cb.like(cb.lower(root.get("shopCode")), pattern, esc),
                        cb.like(cb.lower(root.get("mobile")), pattern, esc),
                        cb.like(cb.lower(root.get("email")), pattern, esc)
                ));
            }

            ShopMasterDTO.FilterData filters = request.getFilters();
            if (filters != null) {
                if (filters.getActive() != null) {
                    predicates.add(cb.equal(root.get("active"), filters.getActive()));
                }

                if (filters.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filters.getStatus()));
                }

                if (filters.getCity() != null && !filters.getCity().isBlank()) {
                    predicates.add(cb.equal(
                            cb.lower(root.get("city")),
                            filters.getCity().trim().toLowerCase(Locale.ROOT)));
                }

                if (filters.getGlobalCategoryUuid() != null
                        && !filters.getGlobalCategoryUuid().isBlank()) {

                    // EXISTS sub-query avoids duplicate rows / broken count query
                    Subquery<Long> sub = query.subquery(Long.class);
                    var subRoot = sub.from(ShopMaster.class);
                    Join<ShopMaster, ShopGlobalCategory> g =
                            subRoot.join("globalCategories");
                    sub.select(subRoot.get("recordId")).where(
                            cb.equal(subRoot.get("recordId"), root.get("recordId")),
                            cb.equal(g.get("uuid"), filters.getGlobalCategoryUuid())
                    );
                    predicates.add(cb.exists(sub));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
