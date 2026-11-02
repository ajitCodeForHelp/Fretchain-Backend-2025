package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.SettingsDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/settings")
@RequiredArgsConstructor
public class SettingsController extends _BaseController {

    private final SettingsService settingsService;

    @GetMapping("/pricing")
    public ResponseEntity<ResponseData> getPricing() {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(settingsService.getPricing())
                        .build()
        );
    }

    @PutMapping("/pricing")
    public ResponseEntity<ResponseData> updatePricing(
            @RequestBody SettingsDTO.Pricing request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.updated")
                        .data(settingsService.updatePricing(request, user))
                        .build()
        );
    }

    @GetMapping("/platform")
    public ResponseEntity<ResponseData> getPlatform() {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(settingsService.getPlatform())
                        .build()
        );
    }

    @PutMapping("/platform")
    public ResponseEntity<ResponseData> updatePlatform(
            @RequestBody SettingsDTO.Platform request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.updated")
                        .data(settingsService.updatePlatform(request, user))
                        .build()
        );
    }
}