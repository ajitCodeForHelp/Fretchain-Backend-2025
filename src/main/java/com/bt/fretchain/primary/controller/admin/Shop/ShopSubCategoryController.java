package com.bt.fretchain.primary.controller.admin.Shop;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopSubCategoryDTO;
import com.bt.fretchain.primary.service.ShopSubCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/shop-subcategory")
@RequiredArgsConstructor
public class ShopSubCategoryController extends _BaseController {

    private final ShopSubCategoryService service;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody ShopSubCategoryDTO.SaveOrUpdate request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.saved")
                .data(service.save(request, getLoggedInUser()))
                .build());
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody ShopSubCategoryDTO.SaveOrUpdate request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.updated")
                .data(service.update(uuid, request, getLoggedInUser()))
                .build());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> getById(
            @PathVariable String uuid) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(service.getById(uuid))
                .build());
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody ShopSubCategoryDTO.ListOrSearchData request) {

        DataTableResponsePacket<ShopSubCategoryDTO.Detail> data =
                service.getListData(request);

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

        service.updateStatus(uuid, active, getLoggedInUser());

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("status.updated")
                .build());
    }
}