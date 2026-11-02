package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BannerRepository extends
        JpaRepository<Banner, Long>,
        JpaSpecificationExecutor<Banner> {

    Optional<Banner> findByUuidAndDeletedFalse(String uuid);

    List<Banner> findByActiveTrueAndDeletedFalseOrderBySortOrderAsc();

    @Query("""
            SELECT b
            FROM Banner b
            WHERE b.active = true
            AND b.deleted = false
            AND (b.startDate IS NULL OR b.startDate <= :now)
            AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.sortOrder ASC
            """)
    List<Banner> findActiveBanners(LocalDateTime now);
}