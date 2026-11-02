package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findTopByOrderByRecordIdAsc();
}