package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantItemDTO;
import com.bt.fretchain.primary.mapper.RestaurantItemMapper;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.repository.RestaurantCategoryRepository;
import com.bt.fretchain.primary.repository.RestaurantItemRepository;
import com.bt.fretchain.primary.repository.RestaurantMasterRepository;
import com.bt.fretchain.primary.repository.RestaurantSubCategoryRepository;
import com.bt.fretchain.primary.service.RestaurantItemService;
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
public class RestaurantItemServiceImpl implements RestaurantItemService {

    private final RestaurantItemRepository repository;
    private final RestaurantMasterRepository restaurantRepository;
    private final RestaurantCategoryRepository categoryRepository;
    private final RestaurantSubCategoryRepository subCategoryRepository;
    private final RestaurantItemMapper mapper;

    @Override
    public RestaurantItemDTO.Detail save(
            RestaurantItemDTO.SaveOrUpdate request,
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
        validatePrice(request);

        RestaurantItem entity = mapper.toEntity(request);
        entity.setRestaurant(restaurant);
        entity.setCategory(category);
        entity.setSubCategory(resolveSubCategory(request, restaurant, category));
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public RestaurantItemDTO.Detail update(
            String uuid,
            RestaurantItemDTO.SaveOrUpdate request,
            SystemUser user) {

        RestaurantItem entity = repository.findByUuidAndDeletedFalse(uuid)
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
        validatePrice(request);

        mapper.updateEntityFromDto(request, entity);
        entity.setRestaurant(restaurant);
        entity.setCategory(category);
        entity.setSubCategory(resolveSubCategory(request, restaurant, category));
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public RestaurantItemDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        RestaurantItem entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());
        repository.save(entity);
    }

    private RestaurantSubCategory resolveSubCategory(
            RestaurantItemDTO.SaveOrUpdate request,
            RestaurantMaster restaurant,
            RestaurantCategory category) {

        if (request.getSubCategoryUuid() == null
                || request.getSubCategoryUuid().isBlank()) {
            return null;
        }

        RestaurantSubCategory subCategory =
                subCategoryRepository.findByUuidAndDeletedFalse(
                        request.getSubCategoryUuid()
                ).orElseThrow(() ->
                        new ResourceNotFoundException("subcategory.notfound"));

        if (!subCategory.getRestaurant().getRecordId().equals(restaurant.getRecordId())
                || !subCategory.getCategory().getRecordId()
                        .equals(category.getRecordId())) {
            throw new IllegalArgumentException(
                    "Subcategory must belong to the selected restaurant and category");
        }

        return subCategory;
    }

    private void validateCategoryBelongsToRestaurant(
            RestaurantCategory category, RestaurantMaster restaurant) {

        if (!category.getRestaurant().getRecordId()
                .equals(restaurant.getRecordId())) {
            throw new IllegalArgumentException(
                    "Category does not belong to the selected restaurant");
        }
    }

    private void validatePrice(RestaurantItemDTO.SaveOrUpdate request) {
        if (request.getPrice() == null
                || request.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        if (request.getDiscountPrice() != null
                && (request.getDiscountPrice().signum() < 0
                || request.getDiscountPrice()
                        .compareTo(request.getPrice()) > 0)) {
            throw new IllegalArgumentException(
                    "Discount price must be between zero and the regular price");
        }
    }
    @Override
    public DataTableResponsePacket<RestaurantItemDTO.Detail> getListData(
            RestaurantItemDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<RestaurantItem> specification =
                createFilterOrSearch(request);

        Page<RestaurantItem> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            RestaurantItemDTO.ListOrSearchData request) {

        Set<String> allowedFields = Set.of(
                "createdAt", "nameEn", "nameFr",
                "price", "displayOrder", "foodType"
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

    private Specification<RestaurantItem> createFilterOrSearch(
            RestaurantItemDTO.ListOrSearchData request) {

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
                        cb.like(cb.lower(root.get("nameFr")), search),
                        cb.like(cb.lower(root.get("itemCode")), search)
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

            if (request.getSubCategoryUuid() != null
                    && !request.getSubCategoryUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("subCategory").get("uuid"),
                        request.getSubCategoryUuid()
                ));
            }

            if (request.getAvailable() != null) {
                predicates.add(cb.equal(
                        root.get("available"),
                        request.getAvailable()
                ));
            }

            if (request.getFoodType() != null) {
                predicates.add(cb.equal(root.get("foodType"), request.getFoodType()));
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