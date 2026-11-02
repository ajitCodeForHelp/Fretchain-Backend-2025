package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.SettingsDTO;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.primary.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final SystemPricingRepository systemPricingRepository;
    private final PlatformSettingRepository platformSettingRepository;
    @Override
    public SettingsDTO.Pricing getPricing() {
        return toPricingDto(
                systemPricingRepository
                        .findTopByOrderByRecordIdAsc()
                        .orElse(new SystemPricing())
        );
    }

    @Override
    public SettingsDTO.Pricing updatePricing(
            SettingsDTO.Pricing request,
            SystemUser user
    ) {
        SystemPricing entity =
                systemPricingRepository
                        .findTopByOrderByRecordIdAsc()
                        .orElse(new SystemPricing());

        entity.setAdminServiceCharge(request.getAdminServiceCharge());
        entity.setInsurancePercentage(request.getInsurancePercentage());
        entity.setHelperCharge(request.getHelperCharge());
        entity.setWaitingChargePerMinute(request.getWaitingChargePerMinute());
        entity.setTaxPercentage(request.getTaxPercentage());
        entity.setCashServicePercentage(request.getCashServicePercentage());

        if (entity.getRecordId() == null) {
            entity.setCreatedBy(user.getRecordId());
        }

        entity.setModifiedBy(user.getRecordId());

        return toPricingDto(
                systemPricingRepository.save(entity)
        );
    }

    @Override
    public SettingsDTO.Platform getPlatform() {
        return toPlatformDto(
                platformSettingRepository
                        .findTopByOrderByRecordIdAsc()
                        .orElse(new PlatformSetting())
        );
    }

    @Override
    public SettingsDTO.Platform updatePlatform(
            SettingsDTO.Platform request,
            SystemUser user
    ) {
        PlatformSetting entity =
                platformSettingRepository
                        .findTopByOrderByRecordIdAsc()
                        .orElse(new PlatformSetting());

        entity.setCompanyName(request.getCompanyName());
        entity.setSupportEmail(request.getSupportEmail());
        entity.setSupportPhone(request.getSupportPhone());
        entity.setSupportWhatsapp(request.getSupportWhatsapp());
        entity.setCompanyAddress(request.getCompanyAddress());
        entity.setCurrencyCode(request.getCurrencyCode());
        entity.setCurrencySymbol(request.getCurrencySymbol());
        entity.setPlatformMaintenanceMode(request.getPlatformMaintenanceMode());
        entity.setPrivacyPolicyUrl(request.getPrivacyPolicyUrl());
        entity.setTermsConditionUrl(request.getTermsConditionUrl());

        if (entity.getRecordId() == null) {
            entity.setCreatedBy(user.getRecordId());
        }

        entity.setModifiedBy(user.getRecordId());

        return toPlatformDto(
                platformSettingRepository.save(entity)
        );
    }
    private SettingsDTO.Pricing toPricingDto(SystemPricing entity) {

        SettingsDTO.Pricing dto = new SettingsDTO.Pricing();

        dto.setAdminServiceCharge(entity.getAdminServiceCharge());
        dto.setInsurancePercentage(entity.getInsurancePercentage());
        dto.setHelperCharge(entity.getHelperCharge());
        dto.setWaitingChargePerMinute(entity.getWaitingChargePerMinute());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setCashServicePercentage(entity.getCashServicePercentage());

        return dto;
    }

    private SettingsDTO.Platform toPlatformDto(PlatformSetting entity) {

        SettingsDTO.Platform dto = new SettingsDTO.Platform();

        dto.setCompanyName(entity.getCompanyName());
        dto.setSupportEmail(entity.getSupportEmail());
        dto.setSupportPhone(entity.getSupportPhone());
        dto.setSupportWhatsapp(entity.getSupportWhatsapp());
        dto.setCompanyAddress(entity.getCompanyAddress());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencySymbol(entity.getCurrencySymbol());
        dto.setPlatformMaintenanceMode(entity.getPlatformMaintenanceMode());
        dto.setPrivacyPolicyUrl(entity.getPrivacyPolicyUrl());
        dto.setTermsConditionUrl(entity.getTermsConditionUrl());

        dto.setInstagramUrl(entity.getInstagramUrl());
        dto.setYoutubeUrl(entity.getYoutubeUrl());
        dto.setFacebookUrl(entity.getFacebookUrl());
        dto.setTwitterUrl(entity.getTwitterUrl());
        dto.setLinkedinUrl(entity.getLinkedinUrl());

        return dto;
    }

}