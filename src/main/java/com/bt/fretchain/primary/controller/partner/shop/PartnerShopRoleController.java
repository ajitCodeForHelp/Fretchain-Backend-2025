package com.bt.fretchain.primary.controller.partner.shop;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopPartnerDTO;
import com.bt.fretchain.primary.serviceImpl.ShopPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Shop login: roles and the activities mapped to each role. */
@RestController
@RequestMapping("/partner/api/shop-role")
@RequiredArgsConstructor
public class PartnerShopRoleController extends _BaseController {

    private final ShopPartnerService service;

    /** All activities that can be mapped to a role. */
    @GetMapping("/activities")
    public ResponseEntity<ResponseData> activities() {
        return ok(service.activityCatalog(), "record.fetched");
    }

    /** Assignable roles for the staff form (excludes the owner role). */
    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> keyValue() {
        ShopPartnerDTO.ListOrSearchData req = new ShopPartnerDTO.ListOrSearchData();
        req.setPageSize(100);
        ShopPartnerDTO.FilterData f = new ShopPartnerDTO.FilterData();
        f.setActive(true);
        req.setFilters(f);
        return ok(service.listRoles(getLoggedInUser(), req).getData().stream()
                .filter(r -> !r.isOwnerRole())
                .map(r -> new KeyValueDto(r.getUuid(), r.getName()))
                .toList(), "record.dropdown.fetched");
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@RequestBody ShopPartnerDTO.ListOrSearchData request) {
        return ok(service.listRoles(getLoggedInUser(), request), "record.list.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> get(@PathVariable String uuid) {
        return ok(service.getRole(getLoggedInUser(), uuid), "record.fetched");
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(@Valid @RequestBody ShopPartnerDTO.RoleSaveOrUpdate request) {
        return ok(service.saveRole(getLoggedInUser(), request), "record.saved");
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(@PathVariable String uuid,
                                               @Valid @RequestBody ShopPartnerDTO.RoleSaveOrUpdate request) {
        return ok(service.updateRole(getLoggedInUser(), uuid, request), "record.updated");
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> status(@PathVariable String uuid, @RequestParam Boolean active) {
        service.updateRoleStatus(getLoggedInUser(), uuid, Boolean.TRUE.equals(active));
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
