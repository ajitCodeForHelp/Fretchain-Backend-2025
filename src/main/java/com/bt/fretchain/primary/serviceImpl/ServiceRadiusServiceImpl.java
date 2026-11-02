package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ServiceRadiusDTO;
import com.bt.fretchain.primary.mapper.ServiceRadiusMapper;
import com.bt.fretchain.primary.pojo.ServiceRadius;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.ServiceRadiusRepository;
import com.bt.fretchain.primary.service.ServiceRadiusService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRadiusServiceImpl implements ServiceRadiusService {

    private final ServiceRadiusRepository serviceRadiusRepository;

    private final ServiceRadiusMapper serviceRadiusMapper;

    @Override
    public ServiceRadiusDTO.Detail save(
            ServiceRadiusDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateRequest(request);
        validateDuplicate(request);

        ServiceRadius entity =
                serviceRadiusMapper.toEntity(request);

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return serviceRadiusMapper.toDetailDto(
                serviceRadiusRepository.save(entity)
        );
    }

    @Override
    public ServiceRadiusDTO.Detail update(
            String uuid,
            ServiceRadiusDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateRequest(request);

        ServiceRadius entity =
                serviceRadiusRepository
                        .findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        validateDuplicateForUpdate(
                request,
                entity
        );

        serviceRadiusMapper.updateEntityFromDto(
                request,
                entity
        );

        entity.setModifiedBy(user.getRecordId());

        return serviceRadiusMapper.toDetailDto(
                serviceRadiusRepository.save(entity)
        );
    }

    @Override
    public ServiceRadiusDTO.Detail getById(
            String uuid
    ) {
        ServiceRadius entity =
                serviceRadiusRepository
                        .findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        return serviceRadiusMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {
        ServiceRadius entity =
                serviceRadiusRepository
                        .findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        serviceRadiusRepository.save(entity);
    }

    @Override
    public DataTableResponsePacket<ServiceRadiusDTO.Detail> getListData(
            ServiceRadiusDTO.ListOrSearchData request
    ) {
        Pageable pageable =
                pageRequestCreate(request);

        Specification<ServiceRadius> specification =
                createFilterOrSearch(request);

        Page<ServiceRadius> page =
                serviceRadiusRepository.findAll(
                        specification,
                        pageable
                );

        return PaginationUtils.createResponse(
                page.map(serviceRadiusMapper::toDetailDto)
        );
    }

    private void validateRequest(
            ServiceRadiusDTO.SaveOrUpdate request
    ) {
        if (request.getRadiusInMeter() != null
                && request.getRadiusInMeter() <= 0) {

            throw new BadRequestException("radius.invalid");
        }

        if (request.getCenterLatitude() != null
                && (request.getCenterLatitude() < -90
                || request.getCenterLatitude() > 90)) {

            throw new BadRequestException("latitude.invalid");
        }

        if (request.getCenterLongitude() != null
                && (request.getCenterLongitude() < -180
                || request.getCenterLongitude() > 180)) {

            throw new BadRequestException("longitude.invalid");
        }
    }

    private void validateDuplicate(
            ServiceRadiusDTO.SaveOrUpdate request
    ) {
        serviceRadiusRepository
                .findByAreaNameIgnoreCaseAndDeletedFalse(
                        request.getAreaName().trim()
                )
                .ifPresent(entity -> {
                    throw new DuplicateResourceException(
                            "area.name.exists"
                    );
                });
    }

    private void validateDuplicateForUpdate(
            ServiceRadiusDTO.SaveOrUpdate request,
            ServiceRadius entity
    ) {
        serviceRadiusRepository
                .findByAreaNameIgnoreCaseAndDeletedFalse(
                        request.getAreaName().trim()
                )
                .ifPresent(existing -> {
                    if (!existing.getRecordId()
                            .equals(entity.getRecordId())) {

                        throw new DuplicateResourceException(
                                "area.name.exists"
                        );
                    }
                });
    }

    private Pageable pageRequestCreate(
            ServiceRadiusDTO.ListOrSearchData request
    ) {
        Sort sort =
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                );

        if (request.getSort() != null
                && request.getSort().getField() != null
                && !request.getSort().getField().isBlank()) {

            Sort.Direction direction =
                    "asc".equalsIgnoreCase(
                            request.getSort().getDirection()
                    )
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            sort =
                    Sort.by(
                            direction,
                            request.getSort().getField()
                    );
        }

        return PageRequest.of(
                request.getPageNumber(),
                request.getPageSize(),
                sort
        );
    }

    private Specification<ServiceRadius> createFilterOrSearch(
            ServiceRadiusDTO.ListOrSearchData request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    cb.equal(
                            root.get("deleted"),
                            false
                    )
            );

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search =
                        "%"
                                + request.getSearch()
                                .toLowerCase()
                                + "%";

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("areaName")),
                                        search
                                ),
                                cb.like(
                                        cb.lower(root.get("cityName")),
                                        search
                                ),
                                cb.like(
                                        cb.lower(root.get("countryName")),
                                        search
                                )
                        )
                );
            }

            if (request.getFilters() != null
                    && request.getFilters().getActive() != null) {

                predicates.add(
                        cb.equal(
                                root.get("active"),
                                request.getFilters().getActive()
                        )
                );
            }

            if (request.getDateFilter() != null) {

                if (request.getDateFilter().getFrom() != null) {
                    predicates.add(
                            cb.greaterThanOrEqualTo(
                                    root.get("createdAt"),
                                    request.getDateFilter()
                                            .getFrom()
                                            .atStartOfDay()
                            )
                    );
                }

                if (request.getDateFilter().getTo() != null) {
                    predicates.add(
                            cb.lessThanOrEqualTo(
                                    root.get("createdAt"),
                                    request.getDateFilter()
                                            .getTo()
                                            .atTime(
                                                    23,
                                                    59,
                                                    59
                                            )
                            )
                    );
                }
            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}