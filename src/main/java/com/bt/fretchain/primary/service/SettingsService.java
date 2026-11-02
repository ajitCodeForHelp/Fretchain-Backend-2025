package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.SettingsDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface SettingsService {

    SettingsDTO.Pricing getPricing();

    SettingsDTO.Pricing updatePricing(
            SettingsDTO.Pricing request,
            SystemUser user
    );

    SettingsDTO.Platform getPlatform();

    SettingsDTO.Platform updatePlatform(
            SettingsDTO.Platform request,
            SystemUser user
    );
}