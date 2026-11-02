package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.mapper.ShopMasterMapper;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.ShopMasterRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.service.ShopMasterService;
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
public class ShopMasterServiceImpl implements ShopMasterService {

    private final ShopMasterRepository repository;
    private final ShopMasterMapper mapper;
    private final SystemUserRepository userRepository;

    @Override
    public ShopMasterDTO.Detail save(
            ShopMasterDTO.SaveOrUpdate request,
            SystemUser user) {

        validateShopCode(request.getShopCode(), null);

        ShopMaster entity = mapper.toEntity(request);

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
    public ShopMasterDTO.Detail update(
            String uuid,
            ShopMasterDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopMaster entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        validateShopCode(request.getShopCode(), entity.getRecordId());

        mapper.updateEntityFromDto(request, entity);

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
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        ShopMaster entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        repository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String lang) {
        return repository.findByActiveTrueAndDeletedFalseOrderByNameEnAsc()
                .stream()
                .map(shop -> new KeyValueDto(
                        shop.getUuid(),
                        "fr".equalsIgnoreCase(lang)
                                ? shop.getNameFr()
                                : shop.getNameEn()
                ))
                .toList();
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
                "shopCode", "mobile", "status"
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

            if (request.getSearch() != null
                    && !request.getSearch().isBlank()) {

                String search = "%"
                        + request.getSearch().trim().toLowerCase()
                        + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), search),
                        cb.like(cb.lower(root.get("nameFr")), search),
                        cb.like(cb.lower(root.get("shopCode")), search),
                        cb.like(cb.lower(root.get("mobile")), search),
                        cb.like(cb.lower(root.get("email")), search)
                ));
            }

            if (request.getFilters() != null) {
                if (request.getFilters().getActive() != null) {
                    predicates.add(cb.equal(
                            root.get("active"),
                            request.getFilters().getActive()
                    ));
                }

                if (request.getFilters().getStatus() != null
                        && !request.getFilters().getStatus().isBlank()) {
                    predicates.add(cb.equal(
                            root.get("status"),
                            request.getFilters().getStatus().trim()
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}