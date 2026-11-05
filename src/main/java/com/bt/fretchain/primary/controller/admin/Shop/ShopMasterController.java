package com.bt.fretchain.primary.controller.admin.Shop;

import com.bt.fretchain.bean.*;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.dto.ShopPartnerDTO;
import com.bt.fretchain.primary.service.ShopMasterService;
import com.bt.fretchain.primary.serviceImpl.ShopPartnerService;
import com.bt.fretchain.primary.serviceImpl.ShopRegistrationService;
import com.bt.fretchain.primary.dto.ShopRegistrationDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/shop")
@RequiredArgsConstructor
public class ShopMasterController extends _BaseController {

    private final ShopMasterService shopService;
    private final ShopPartnerService partnerService;
    private final ShopRegistrationService registrationService;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody ShopMasterDTO.SaveOrUpdate request) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.saved")
                .data(shopService.save(request, user))
                .build());
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody ShopMasterDTO.SaveOrUpdate request) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.updated")
                .data(shopService.update(uuid, request, user))
                .build());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> getById(
            @PathVariable String uuid) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(shopService.getById(uuid))
                .build());
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody ShopMasterDTO.ListOrSearchData request) {

        DataTableResponsePacket<ShopMasterDTO.Detail> data =
                shopService.getListData(request);

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.list.fetched")
                .data(data)
                .build());
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> updateStatus(
            @PathVariable String uuid,
            @RequestParam Boolean active) {

        shopService.updateStatus(uuid, active, getLoggedInUser());

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("status.updated")
                .build());
    }

    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> listInKeyValue(
            @RequestHeader(value = "Accept-Language", defaultValue = "en")
            String lang) {

        List<KeyValueDto> data = shopService.getKeyValueList(lang);

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.dropdown.fetched")
                .data(data)
                .build());
    }

    /** Approve / reject / suspend (reason required for reject and suspend). */
    @PatchMapping("/{uuid}/approval")
    public ResponseEntity<ResponseData> approval(
            @PathVariable String uuid,
            @Valid @RequestBody ShopRegistrationDTO.ApprovalUpdate request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("status.updated")
                .data(registrationService.changeStatus(uuid, request, getLoggedInUser()))
                .build());
    }

    /** Existing shop owners, for "add shop for an existing owner". */
    @GetMapping("/owners/list-in-key-value")
    public ResponseEntity<ResponseData> owners() {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.dropdown.fetched")
                .data(partnerService.ownerKeyValue())
                .build());
    }

    /** Owner login of the shop (null when not created yet). */
    @GetMapping("/{uuid}/owner")
    public ResponseEntity<ResponseData> getOwner(@PathVariable String uuid) {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(partnerService.getOwner(uuid))
                .build());
    }

    /** Create the owner login, or update its details / reset its password. */
    @PutMapping("/{uuid}/owner")
    public ResponseEntity<ResponseData> saveOwner(
            @PathVariable String uuid,
            @Valid @RequestBody ShopPartnerDTO.OwnerSave request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.saved")
                .data(partnerService.saveOwner(uuid, request, getLoggedInUser()))
                .build());
    }
}
