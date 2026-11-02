package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopSubCategoryDTO;
import com.bt.fretchain.primary.mapper.ShopSubCategoryMapper;
import com.bt.fretchain.primary.pojo.ShopCategory;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.ShopSubCategory;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.ShopCategoryRepository;
import com.bt.fretchain.primary.repository.ShopMasterRepository;
import com.bt.fretchain.primary.repository.ShopSubCategoryRepository;
import com.bt.fretchain.primary.service.ShopSubCategoryService;
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
public class ShopSubCategoryServiceImpl
        implements ShopSubCategoryService {

    private final ShopSubCategoryRepository repository;
    private final ShopMasterRepository shopRepository;
    private final ShopCategoryRepository categoryRepository;
    private final ShopSubCategoryMapper mapper;

    @Override
    public ShopSubCategoryDTO.Detail save(
            ShopSubCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(
                request.getShopUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("shop.notfound"));

        ShopCategory category = categoryRepository.findByUuidAndDeletedFalse(
                request.getCategoryUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("category.notfound"));

        validateCategoryBelongsToShop(category, shop);

        ShopSubCategory entity = mapper.toEntity(request);
        entity.setShop(shop);
        entity.setCategory(category);
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public ShopSubCategoryDTO.Detail update(
            String uuid,
            ShopSubCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopSubCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(
                request.getShopUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("shop.notfound"));

        ShopCategory category = categoryRepository.findByUuidAndDeletedFalse(
                request.getCategoryUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("category.notfound"));

        validateCategoryBelongsToShop(category, shop);

        mapper.updateEntityFromDto(request, entity);
        entity.setShop(shop);
        entity.setCategory(category);
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public ShopSubCategoryDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        ShopSubCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());
        repository.save(entity);
    }

    private void validateCategoryBelongsToShop(
            ShopCategory category, ShopMaster shop) {

        if (!category.getShop().getRecordId()
                .equals(shop.getRecordId())) {
            throw new IllegalArgumentException(
                    "Category does not belong to the selected shop");
        }
    }

    @Override
    public DataTableResponsePacket<ShopSubCategoryDTO.Detail> getListData(
            ShopSubCategoryDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<ShopSubCategory> specification =
                createFilterOrSearch(request);

        Page<ShopSubCategory> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            ShopSubCategoryDTO.ListOrSearchData request) {

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

    private Specification<ShopSubCategory> createFilterOrSearch(
            ShopSubCategoryDTO.ListOrSearchData request) {

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

            if (request.getShopUuid() != null
                    && !request.getShopUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("shop").get("uuid"),
                        request.getShopUuid()
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