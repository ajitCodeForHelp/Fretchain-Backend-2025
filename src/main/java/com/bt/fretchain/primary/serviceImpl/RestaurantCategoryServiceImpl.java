package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantCategoryDTO;
import com.bt.fretchain.primary.mapper.RestaurantCategoryMapper;
import com.bt.fretchain.primary.pojo.RestaurantCategory;
import com.bt.fretchain.primary.pojo.RestaurantGlobalCategory;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.RestaurantCategoryRepository;
import com.bt.fretchain.primary.repository.RestaurantMasterRepository;
import com.bt.fretchain.primary.service.RestaurantCategoryService;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RestaurantCategoryServiceImpl implements RestaurantCategoryService {

    private final RestaurantCategoryRepository repository;
    private final RestaurantMasterRepository restaurantRepository;
    private final RestaurantCategoryMapper mapper;

    @Override
    @Transactional
    public RestaurantCategoryDTO.Detail save(
            RestaurantCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(
                request.getRestaurantUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("restaurant.notfound"));

        validateName(request, restaurant.getRecordId(), null);

        RestaurantCategory entity = mapper.toEntity(request);
        entity.setRestaurant(restaurant);
        entity.setGlobalCategory(resolveGlobalCategory(request, restaurant));
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    @Transactional
    public RestaurantCategoryDTO.Detail update(
            String uuid,
            RestaurantCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        RestaurantCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(
                request.getRestaurantUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("restaurant.notfound"));

        validateName(request, restaurant.getRecordId(), entity.getRecordId());

        mapper.updateEntityFromDto(request, entity);
        entity.setRestaurant(restaurant);
        entity.setGlobalCategory(resolveGlobalCategory(request, restaurant));
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public RestaurantCategoryDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        RestaurantCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());
        repository.save(entity);
    }

    private void validateName(
            RestaurantCategoryDTO.SaveOrUpdate request,
            Long restaurantId,
            Long currentId) {

        String name = request.getNameEn().trim();

        boolean exists = currentId == null
                ? repository.existsByRestaurant_RecordIdAndNameEnIgnoreCaseAndDeletedFalse(
                        restaurantId, name)
                : repository.existsByRestaurant_RecordIdAndNameEnIgnoreCaseAndDeletedFalseAndRecordIdNot(
                        restaurantId, name, currentId);

        if (exists) {
            throw new DuplicateResourceException("restaurant.category.name.exists");
        }
    }

    /**
     * Optional link of a restaurant section to a global category. The global category
     * must be one the restaurant is assigned to (keeps customer filtering consistent).
     */
    private RestaurantGlobalCategory resolveGlobalCategory(
            RestaurantCategoryDTO.SaveOrUpdate request, RestaurantMaster restaurant) {

        if (request.getGlobalCategoryUuid() == null
                || request.getGlobalCategoryUuid().isBlank()) {
            return null;
        }

        return restaurant.getGlobalCategories().stream()
                .filter(g -> !g.isDeleted()
                        && g.getUuid().equals(request.getGlobalCategoryUuid().trim()))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException("restaurant.global.category.notassigned"));
    }

    @Override
    public DataTableResponsePacket<RestaurantCategoryDTO.Detail> getListData(
            RestaurantCategoryDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<RestaurantCategory> specification =
                createFilterOrSearch(request);

        Page<RestaurantCategory> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            RestaurantCategoryDTO.ListOrSearchData request) {

        Set<String> allowedFields = Set.of(
                "createdAt", "nameEn", "nameFr", "displayOrder"
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
                pageNumber, pageSize, Sort.by(direction, field));
    }

    private Specification<RestaurantCategory> createFilterOrSearch(
            RestaurantCategoryDTO.ListOrSearchData request) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            if (request.getSearch() != null
                    && !request.getSearch().isBlank()) {

                String search = SearchUtils.containsPattern(request.getSearch());
                char esc = SearchUtils.LIKE_ESCAPE;

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), search, esc),
                        cb.like(cb.lower(root.get("nameFr")), search, esc)
                ));
            }

            if (request.getRestaurantUuid() != null
                    && !request.getRestaurantUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("restaurant").get("uuid"),
                        request.getRestaurantUuid()
                ));
            }

            if (request.getFilters() != null
                    && request.getFilters().getActive() != null) {

                predicates.add(cb.equal(
                        root.get("active"),
                        request.getFilters().getActive()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}