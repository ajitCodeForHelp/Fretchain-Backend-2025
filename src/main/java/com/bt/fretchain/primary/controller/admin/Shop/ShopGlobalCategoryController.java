package com.bt.fretchain.primary.controller.admin.Shop;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopGlobalCategoryDTO;
import com.bt.fretchain.primary.pojo.enums.BusinessType;
import com.bt.fretchain.primary.service.ShopGlobalCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin: platform-level categories used to bifurcate stores
 * (Grocery, Pharmacy... for SHOP; Pizza, Biryani... for RESTAURANT).
 */
@RestController
@RequestMapping("/admin/api/shop-global-category")
@RequiredArgsConstructor
public class ShopGlobalCategoryController extends _BaseController {

    private final ShopGlobalCategoryService service;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody ShopGlobalCategoryDTO.SaveOrUpdate request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.saved")
                .data(service.save(request, getLoggedInUser()))
                .build());
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody ShopGlobalCategoryDTO.SaveOrUpdate request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.updated")
                .data(service.update(uuid, request, getLoggedInUser()))
                .build());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> getById(@PathVariable String uuid) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(service.getById(uuid))
                .build());
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody ShopGlobalCategoryDTO.ListOrSearchData request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.list.fetched")
                .data(service.getListData(request))
                .build());
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> updateStatus(
            @PathVariable String uuid,
            @RequestParam Boolean active) {

        service.updateStatus(uuid, active, getLoggedInUser());

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("status.updated")
                .build());
    }

    /** e.g. GET /admin/api/shop-global-category/list-in-key-value?businessType=RESTAURANT */
    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> listInKeyValue(
            @RequestParam BusinessType businessType,
            @RequestHeader(value = "Accept-Language", defaultValue = "en")
            String lang) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.dropdown.fetched")
                .data(service.getKeyValueList(businessType, lang))
                .build());
    }
}
