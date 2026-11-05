package com.bt.fretchain.primary.controller.admin.Restaurant;

import com.bt.fretchain.bean.*;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.RestaurantMasterDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.dto.RestaurantPartnerDTO;
import com.bt.fretchain.primary.service.RestaurantMasterService;
import com.bt.fretchain.primary.serviceImpl.RestaurantPartnerService;
import com.bt.fretchain.primary.serviceImpl.RestaurantRegistrationService;
import com.bt.fretchain.primary.dto.RestaurantRegistrationDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/restaurant")
@RequiredArgsConstructor
public class RestaurantMasterController extends _BaseController {

    private final RestaurantMasterService restaurantService;
    private final RestaurantPartnerService partnerService;
    private final RestaurantRegistrationService registrationService;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody RestaurantMasterDTO.SaveOrUpdate request) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.saved")
                .data(restaurantService.save(request, user))
                .build());
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody RestaurantMasterDTO.SaveOrUpdate request) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.updated")
                .data(restaurantService.update(uuid, request, user))
                .build());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> getById(
            @PathVariable String uuid) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(restaurantService.getById(uuid))
                .build());
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody RestaurantMasterDTO.ListOrSearchData request) {

        DataTableResponsePacket<RestaurantMasterDTO.Detail> data =
                restaurantService.getListData(request);

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

        restaurantService.updateStatus(uuid, active, getLoggedInUser());

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("status.updated")
                .build());
    }

    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> listInKeyValue(
            @RequestHeader(value = "Accept-Language", defaultValue = "en")
            String lang) {

        List<KeyValueDto> data = restaurantService.getKeyValueList(lang);

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
            @Valid @RequestBody RestaurantRegistrationDTO.ApprovalUpdate request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("status.updated")
                .data(registrationService.changeStatus(uuid, request, getLoggedInUser()))
                .build());
    }

    /** Existing restaurant owners, for "add restaurant for an existing owner". */
    @GetMapping("/owners/list-in-key-value")
    public ResponseEntity<ResponseData> owners() {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.dropdown.fetched")
                .data(partnerService.ownerKeyValue())
                .build());
    }

    /** Owner login of the restaurant (null when not created yet). */
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
            @Valid @RequestBody RestaurantPartnerDTO.OwnerSave request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.saved")
                .data(partnerService.saveOwner(uuid, request, getLoggedInUser()))
                .build());
    }
}
