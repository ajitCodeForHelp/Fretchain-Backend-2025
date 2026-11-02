package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantGlobalCategoryDTO;
import com.bt.fretchain.primary.mapper.RestaurantGlobalCategoryMapper;
import com.bt.fretchain.primary.pojo.RestaurantGlobalCategory;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.RestaurantGlobalCategoryRepository;
import com.bt.fretchain.primary.service.RestaurantGlobalCategoryService;
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
public class RestaurantGlobalCategoryServiceImpl implements RestaurantGlobalCategoryService {

    private final RestaurantGlobalCategoryRepository repository;
    private final RestaurantGlobalCategoryMapper mapper;

    @Override
    @Transactional
    public RestaurantGlobalCategoryDTO.Detail save(
            RestaurantGlobalCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        validateName(request, null);

        RestaurantGlobalCategory entity = mapper.toEntity(request);
        entity.setNameEn(request.getNameEn().trim());
        if (entity.getDisplayOrder() == null) {
            entity.setDisplayOrder(0);
        }
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    @Transactional
    public RestaurantGlobalCategoryDTO.Detail update(
            String uuid,
            RestaurantGlobalCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        RestaurantGlobalCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        validateName(request, entity.getRecordId());

        mapper.updateEntityFromDto(request, entity);
        entity.setNameEn(request.getNameEn().trim());
        if (entity.getDisplayOrder() == null) {
            entity.setDisplayOrder(0);
        }
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public RestaurantGlobalCategoryDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    @Transactional
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        RestaurantGlobalCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(Boolean.TRUE.equals(active));
        entity.setModifiedBy(user.getRecordId());
        repository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String lang) {
        return repository
                .findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameEnAsc()
                .stream()
                .map(c -> new KeyValueDto(
                        c.getUuid(),
                        "fr".equalsIgnoreCase(lang) && c.getNameFr() != null
                                ? c.getNameFr()
                                : c.getNameEn()))
                .toList();
    }

    private void validateName(
            RestaurantGlobalCategoryDTO.SaveOrUpdate request, Long currentId) {

        String name = request.getNameEn().trim();

        boolean exists = currentId == null
                ? repository.existsByNameEnIgnoreCaseAndDeletedFalse(name)
                : repository.existsByNameEnIgnoreCaseAndDeletedFalseAndRecordIdNot(
                        name, currentId);

        if (exists) {
            throw new DuplicateResourceException("restaurant.global.category.name.exists");
        }
    }

    @Override
    public DataTableResponsePacket<RestaurantGlobalCategoryDTO.Detail> getListData(
            RestaurantGlobalCategoryDTO.ListOrSearchData request) {

        Page<RestaurantGlobalCategory> page = repository.findAll(
                createFilterOrSearch(request), pageRequestCreate(request));

        return PaginationUtils.createResponse(page.map(mapper::toDetailDto));
    }

    private Pageable pageRequestCreate(
            RestaurantGlobalCategoryDTO.ListOrSearchData request) {

        Set<String> allowedFields = Set.of(
                "createdAt", "nameEn", "nameFr", "displayOrder"
        );

        String field = "displayOrder";
        Sort.Direction direction = Sort.Direction.ASC;

        if (request.getSort() != null
                && request.getSort().getField() != null
                && allowedFields.contains(request.getSort().getField())) {

            field = request.getSort().getField();
            direction = "desc".equalsIgnoreCase(request.getSort().getDirection())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
        }

        int pageNumber = Math.max(
                0, request.getPageNumber() == null ? 0 : request.getPageNumber());

        int pageSize = request.getPageSize() == null
                ? 10 : Math.max(1, Math.min(100, request.getPageSize()));

        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, field));
    }

    private Specification<RestaurantGlobalCategory> createFilterOrSearch(
            RestaurantGlobalCategoryDTO.ListOrSearchData request) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            String pattern = SearchUtils.containsPattern(request.getSearch());
            if (pattern != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), pattern, SearchUtils.LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("nameFr")), pattern, SearchUtils.LIKE_ESCAPE)
                ));
            }

            if (request.getFilters() != null
                    && request.getFilters().getActive() != null) {
                predicates.add(cb.equal(
                        root.get("active"), request.getFilters().getActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
