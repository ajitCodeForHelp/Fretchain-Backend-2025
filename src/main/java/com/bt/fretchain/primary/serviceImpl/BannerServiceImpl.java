package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.BannerDTO;
import com.bt.fretchain.primary.mapper.BannerMapper;
import com.bt.fretchain.primary.pojo.Banner;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.BannerRepository;
import com.bt.fretchain.primary.service.BannerService;
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
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    private final BannerMapper bannerMapper;

    @Override
    public BannerDTO.Detail save(
            BannerDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateDates(request);

        Banner entity = bannerMapper.toEntity(request);

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        Banner saved = bannerRepository.save(entity);

        return bannerMapper.toDetailDto(saved);
    }

    @Override
    public BannerDTO.Detail update(
            String uuid,
            BannerDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateDates(request);

        Banner entity =
                bannerRepository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        bannerMapper.updateEntityFromDto(request, entity);

        entity.setModifiedBy(user.getRecordId());

        return bannerMapper.toDetailDto(
                bannerRepository.save(entity)
        );
    }

    @Override
    public BannerDTO.Detail getById(String uuid) {

        Banner entity =
                bannerRepository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        return bannerMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {
        Banner entity =
                bannerRepository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        bannerRepository.save(entity);
    }

    @Override
    public DataTableResponsePacket<BannerDTO.Detail> getListData(
            BannerDTO.ListOrSearchData request
    ) {
        Pageable pageable = pageRequestCreate(request);

        Specification<Banner> specification =
                createFilterOrSearch(request);

        Page<Banner> page =
                bannerRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(bannerMapper::toDetailDto)
        );
    }

    private void validateDates(
            BannerDTO.SaveOrUpdate request
    ) {
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {

            throw new BadRequestException("date.range.invalid");
        }
    }

    private Pageable pageRequestCreate(
            BannerDTO.ListOrSearchData request
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        if (request.getSort() != null
                && request.getSort().getField() != null
                && !request.getSort().getField().isBlank()) {

            Sort.Direction direction =
                    "asc".equalsIgnoreCase(request.getSort().getDirection())
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            sort = Sort.by(direction, request.getSort().getField());
        }

        return PageRequest.of(
                request.getPageNumber(),
                request.getPageSize(),
                sort
        );
    }

    private Specification<Banner> createFilterOrSearch(
            BannerDTO.ListOrSearchData request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleted"), false));

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search =
                        "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("titleEn")), search),
                                cb.like(cb.lower(root.get("titleFr")), search)
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
                                            .atTime(23, 59, 59)
                            )
                    );
                }
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}