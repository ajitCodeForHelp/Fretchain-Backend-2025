package com.bt.fretchain.primary.controller.admin.Restaurant;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.RestaurantGlobalCategoryDTO;
import com.bt.fretchain.primary.service.RestaurantGlobalCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin: cuisines used to bifurcate restaurants (Pizza, Biryani, Chinese...). */
@RestController
@RequestMapping("/admin/api/restaurant-global-category")
@RequiredArgsConstructor
public class RestaurantGlobalCategoryController extends _BaseController {

    private final RestaurantGlobalCategoryService service;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody RestaurantGlobalCategoryDTO.SaveOrUpdate request) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.saved")
                .data(service.save(request, getLoggedInUser()))
                .build());
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody RestaurantGlobalCategoryDTO.SaveOrUpdate request) {

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
            @Valid @RequestBody RestaurantGlobalCategoryDTO.ListOrSearchData request) {

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

    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> listInKeyValue(
            @RequestHeader(value = "Accept-Language", defaultValue = "en")
            String lang) {

        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.dropdown.fetched")
                .data(service.getKeyValueList(lang))
                .build());
    }
}
