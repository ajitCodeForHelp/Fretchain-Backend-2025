package com.bt.fretchain.primary.controller.partner.shop;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopPartnerDTO;
import com.bt.fretchain.primary.serviceImpl.ShopPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Shop login: staff logins of the shop and their roles. */
@RestController
@RequestMapping("/partner/api/shop-staff")
@RequiredArgsConstructor
public class PartnerShopStaffController extends _BaseController {

    private final ShopPartnerService service;

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@RequestBody ShopPartnerDTO.ListOrSearchData request) {
        return ok(service.listStaff(getLoggedInUser(), request), "record.list.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> get(@PathVariable String uuid) {
        return ok(service.getStaff(getLoggedInUser(), uuid), "record.fetched");
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(@Valid @RequestBody ShopPartnerDTO.StaffSaveOrUpdate request) {
        return ok(service.saveStaff(getLoggedInUser(), request), "record.saved");
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(@PathVariable String uuid,
                                               @Valid @RequestBody ShopPartnerDTO.StaffSaveOrUpdate request) {
        return ok(service.updateStaff(getLoggedInUser(), uuid, request), "record.updated");
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> status(@PathVariable String uuid, @RequestParam Boolean active) {
        service.updateStaffStatus(getLoggedInUser(), uuid, Boolean.TRUE.equals(active));
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
