package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantSubCategoryDTO;
import com.bt.fretchain.primary.mapper.RestaurantSubCategoryMapper;
import com.bt.fretchain.primary.pojo.RestaurantCategory;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import com.bt.fretchain.primary.pojo.RestaurantSubCategory;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.RestaurantCategoryRepository;
import com.bt.fretchain.primary.repository.RestaurantMasterRepository;
import com.bt.fretchain.primary.repository.RestaurantSubCategoryRepository;
import com.bt.fretchain.primary.service.RestaurantSubCategoryService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RestaurantSubCategoryServiceImpl
        implements RestaurantSubCategoryService {

    private final RestaurantSubCategoryRepository repository;
    private final RestaurantMasterRepository restaurantRepository;
    private final RestaurantCategoryRepository categoryRepository;
    private final RestaurantSubCategoryMapper mapper;

    @Override
    public RestaurantSubCategoryDTO.Detail save(
            RestaurantSubCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(
                request.getRestaurantUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("restaurant.notfound"));

        RestaurantCategory category = categoryRepository.findByUuidAndDeletedFalse(
                request.getCategoryUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("category.notfound"));

        validateCategoryBelongsToRestaurant(category, restaurant);

        RestaurantSubCategory entity = mapper.toEntity(request);
        entity.setRestaurant(restaurant);
        entity.setCategory(category);
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public RestaurantSubCategoryDTO.Detail update(
            String uuid,
            RestaurantSubCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        RestaurantSubCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(
                request.getRestaurantUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("restaurant.notfound"));

        RestaurantCategory category = categoryRepository.findByUuidAndDeletedFalse(
                request.getCategoryUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("category.notfound"));

        validateCategoryBelongsToRestaurant(category, restaurant);

        mapper.updateEntityFromDto(request, entity);
        entity.setRestaurant(restaurant);
        entity.setCategory(category);
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public RestaurantSubCategoryDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        RestaurantSubCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());
        repository.save(entity);
    }

    private void validateCategoryBelongsToRestaurant(
            RestaurantCategory category, RestaurantMaster restaurant) {

        if (!category.getRestaurant().getRecordId()
                .equals(restaurant.getRecordId())) {
            throw new IllegalArgumentException(
                    "Category does not belong to the selected restaurant");
        }
    }

    @Override
    public DataTableResponsePacket<RestaurantSubCategoryDTO.Detail> getListData(
            RestaurantSubCategoryDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<RestaurantSubCategory> specification =
                createFilterOrSearch(request);

        Page<RestaurantSubCategory> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            RestaurantSubCategoryDTO.ListOrSearchData request) {

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

    private Specification<RestaurantSubCategory> createFilterOrSearch(
            RestaurantSubCategoryDTO.ListOrSearchData request) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            if (request.getSearch() != null
                    && !request.getSearch().isBlank()) {

                String search = "%"
                        + request.getSearch().trim().toLowerCase()
                        + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), search),
                        cb.like(cb.lower(root.get("nameFr")), search)
                ));
            }

            if (request.getRestaurantUuid() != null
                    && !request.getRestaurantUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("restaurant").get("uuid"),
                        request.getRestaurantUuid()
                ));
            }

            if (request.getCategoryUuid() != null
                    && !request.getCategoryUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("category").get("uuid"),
                        request.getCategoryUuid()
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