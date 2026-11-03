package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantMasterDTO;
import com.bt.fretchain.primary.dto.RestaurantPartnerDTO;
import com.bt.fretchain.primary.mapper.RestaurantMasterMapper;
import com.bt.fretchain.primary.pojo.RestaurantCategory;
import com.bt.fretchain.primary.pojo.RestaurantGlobalCategory;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.RestaurantStatus;
import com.bt.fretchain.primary.repository.RestaurantCategoryRepository;
import com.bt.fretchain.primary.repository.RestaurantGlobalCategoryRepository;
import com.bt.fretchain.primary.repository.RestaurantMasterRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.service.RestaurantMasterService;
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
public class RestaurantMasterServiceImpl implements RestaurantMasterService {

    private final RestaurantMasterRepository repository;
    private final RestaurantMasterMapper mapper;
    private final SystemUserRepository userRepository;
    private final RestaurantGlobalCategoryRepository globalCategoryRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final RestaurantPartnerService partnerService;

    @Override
    @Transactional
    public RestaurantMasterDTO.Detail save(
            RestaurantMasterDTO.SaveOrUpdate request,
            SystemUser user) {

        validateRestaurantCode(request.getRestaurantCode(), null);
        validateOwner(request);
        validateTimings(request);

        RestaurantMaster entity = mapper.toEntity(request);
        applyDefaults(entity, RestaurantStatus.PENDING);

        entity.setGlobalCategories(resolveGlobalCategories(
                request.getGlobalCategoryUuids()));


        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());
        entity = repository.save(entity);

        // Owner login: system_user (RESTAURANT_USER) + Owner role, linked as restaurant.owner
        RestaurantPartnerDTO.OwnerSave owner = new RestaurantPartnerDTO.OwnerSave();
        owner.setFullName(request.getOwnerName());
        owner.setEmail(request.getOwnerEmail());
        owner.setMobile(request.getOwnerMobile());
        owner.setPassword(request.getOwnerPassword());
        partnerService.saveOwner(entity.getUuid(), owner, user);

        return mapper.toDetailDto(entity);
    }

    @Override
    @Transactional
    public RestaurantMasterDTO.Detail update(
            String uuid,
            RestaurantMasterDTO.SaveOrUpdate request,
            SystemUser user) {

        RestaurantMaster entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        validateRestaurantCode(request.getRestaurantCode(), entity.getRecordId());
        validateTimings(request);

        RestaurantStatus previousStatus = entity.getStatus();

        mapper.updateEntityFromDto(request, entity);
        applyDefaults(entity, previousStatus);

        Set<RestaurantGlobalCategory> globalCategories = resolveGlobalCategories(
                request.getGlobalCategoryUuids());

        // Replace contents (keeps the Hibernate-managed collection instance)
        entity.getGlobalCategories().clear();
        entity.getGlobalCategories().addAll(globalCategories);

        unlinkRemovedGlobalCategories(entity, globalCategories, user);


        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public RestaurantMasterDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    @Transactional
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        RestaurantMaster entity = repository.findByUuidAndDeletedFalse(uuid)
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
                .map(restaurant -> new KeyValueDto(
                        restaurant.getUuid(),
                        "fr".equalsIgnoreCase(lang) && restaurant.getNameFr() != null
                                ? restaurant.getNameFr()
                                : restaurant.getNameEn()
                ))
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** All requested global categories must exist and be active. */
    private Set<RestaurantGlobalCategory> resolveGlobalCategories(List<String> uuids) {

        Set<String> distinct = uuids == null ? Set.of() : uuids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (distinct.isEmpty()) {
            throw new BadRequestException("restaurant.global.category.required");
        }

        List<RestaurantGlobalCategory> found =
                globalCategoryRepository.findByUuidInAndDeletedFalse(distinct);

        if (found.size() != distinct.size()) {
            throw new ResourceNotFoundException("restaurant.global.category.notfound");
        }

        for (RestaurantGlobalCategory category : found) {
            if (!category.isActive()) {
                throw new BadRequestException("restaurant.global.category.invalid");
            }
        }

        return new LinkedHashSet<>(found);
    }

    /**
     * If a global category is removed from the store, restaurant sections that pointed
     * to it lose the link (they still exist, just unmapped).
     */
    private void unlinkRemovedGlobalCategories(
            RestaurantMaster restaurant, Set<RestaurantGlobalCategory> current, SystemUser user) {

        Set<Long> currentIds = current.stream()
                .map(RestaurantGlobalCategory::getRecordId)
                .collect(Collectors.toSet());

        for (RestaurantCategory section : restaurantCategoryRepository
                .findByRestaurant_RecordIdAndGlobalCategoryIsNotNullAndDeletedFalse(
                        restaurant.getRecordId())) {

            if (!currentIds.contains(section.getGlobalCategory().getRecordId())) {
                section.setGlobalCategory(null);
                section.setModifiedBy(user.getRecordId());
                restaurantCategoryRepository.save(section);
            }
        }
    }

    private void applyDefaults(RestaurantMaster entity, RestaurantStatus statusIfMissing) {
        if (entity.getStatus() == null) {
            entity.setStatus(statusIfMissing == null ? RestaurantStatus.PENDING : statusIfMissing);
        }
        if (entity.getAcceptingOrders() == null) {
            entity.setAcceptingOrders(true);
        }
        if (entity.getCommissionPercentage() == null) {
            entity.setCommissionPercentage(BigDecimal.ZERO);
        }
    }

    /** The owner login is created together with the restaurant; checked before anything is saved. */
    private void validateOwner(RestaurantMasterDTO.SaveOrUpdate request) {
        if (isBlank(request.getOwnerName()) || isBlank(request.getOwnerEmail())
                || isBlank(request.getOwnerPassword())) {
            throw new BadRequestException("owner.required");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getOwnerEmail().trim())) {
            throw new DuplicateResourceException("user.email.exists");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateTimings(RestaurantMasterDTO.SaveOrUpdate request) {
        boolean onlyOne = (request.getOpeningTime() == null)
                != (request.getClosingTime() == null);

        if (onlyOne) {
            throw new BadRequestException("restaurant.timing.invalid");
        }
        if (request.getOpeningTime() != null
                && request.getOpeningTime().equals(request.getClosingTime())) {
            throw new BadRequestException("restaurant.timing.invalid");
        }
    }

    private void validateRestaurantCode(String code, Long currentId) {
        if (code == null || code.isBlank()) {
            return;
        }

        repository.findByRestaurantCodeIgnoreCaseAndDeletedFalse(code.trim())
                .ifPresent(existing -> {
                    if (currentId == null
                            || !existing.getRecordId().equals(currentId)) {
                        throw new DuplicateResourceException(
                                "restaurant.code.exists");
                    }
                });
    }

    // ------------------------------------------------------------------
    // Admin list
    // ------------------------------------------------------------------

    @Override
    public DataTableResponsePacket<RestaurantMasterDTO.Detail> getListData(
            RestaurantMasterDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<RestaurantMaster> specification =
                createFilterOrSearch(request);

        Page<RestaurantMaster> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            RestaurantMasterDTO.ListOrSearchData request) {

        Set<String> allowedFields = Set.of(
                "createdAt", "nameEn", "nameFr",
                "restaurantCode", "mobile", "status",
                "city", "rating", "costForTwo"
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

    private Specification<RestaurantMaster> createFilterOrSearch(
            RestaurantMasterDTO.ListOrSearchData request) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            String pattern = SearchUtils.containsPattern(request.getSearch());
            if (pattern != null) {
                char esc = SearchUtils.LIKE_ESCAPE;
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), pattern, esc),
                        cb.like(cb.lower(root.get("nameFr")), pattern, esc),
                        cb.like(cb.lower(root.get("restaurantCode")), pattern, esc),
                        cb.like(cb.lower(root.get("mobile")), pattern, esc),
                        cb.like(cb.lower(root.get("email")), pattern, esc)
                ));
            }

            RestaurantMasterDTO.FilterData filters = request.getFilters();
            if (filters != null) {
                if (filters.getActive() != null) {
                    predicates.add(cb.equal(root.get("active"), filters.getActive()));
                }

                if (filters.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filters.getStatus()));
                }

                if (filters.getFoodType() != null) {
                    predicates.add(cb.equal(root.get("foodType"), filters.getFoodType()));
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
                    var subRoot = sub.from(RestaurantMaster.class);
                    Join<RestaurantMaster, RestaurantGlobalCategory> g =
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
