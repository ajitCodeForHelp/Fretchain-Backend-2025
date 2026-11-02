package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.CustomerMarketplaceDTO;
import com.bt.fretchain.primary.pojo.enums.BusinessType;
import com.bt.fretchain.primary.service.CustomerMarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Customer browsing for shops and restaurants. {type} = "shop" or "restaurant".
 *
 *   GET  /customer/api/marketplace/{type}/categories                 global category chips
 *   POST /customer/api/marketplace/{type}/nearby                     nearby stores (filter + sort)
 *   GET  /customer/api/marketplace/{type}/shops/{uuid}               store header + category tree
 *   GET  /customer/api/marketplace/{type}/shops/{uuid}/menu          full menu with items (restaurants)
 *   POST /customer/api/marketplace/{type}/shops/{uuid}/items         items of a category/subcategory (paged)
 *   POST /customer/api/marketplace/{type}/search                     categories + stores + items
 */
@RestController
@RequestMapping("/customer/api/marketplace/{type}")
@RequiredArgsConstructor
public class CustomerMarketplaceController {

    private final CustomerMarketplaceService service;

    @GetMapping("/categories")
    public ResponseEntity<ResponseData> categories(@PathVariable String type) {

        return ok(service.getGlobalCategories(BusinessType.fromPath(type)));
    }

    @PostMapping("/nearby")
    public ResponseEntity<ResponseData> nearby(
            @PathVariable String type,
            @Valid @RequestBody CustomerMarketplaceDTO.NearbyShopRequest request) {

        return ok(service.getNearbyShops(BusinessType.fromPath(type), request));
    }

    @GetMapping("/shops/{shopUuid}")
    public ResponseEntity<ResponseData> shopDetail(
            @PathVariable String type,
            @PathVariable String shopUuid,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        return ok(service.getShopDetail(
                BusinessType.fromPath(type), shopUuid, latitude, longitude));
    }

    @GetMapping("/shops/{shopUuid}/menu")
    public ResponseEntity<ResponseData> shopMenu(
            @PathVariable String type,
            @PathVariable String shopUuid,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        return ok(service.getShopMenu(
                BusinessType.fromPath(type), shopUuid, latitude, longitude));
    }

    @PostMapping("/shops/{shopUuid}/items")
    public ResponseEntity<ResponseData> shopItems(
            @PathVariable String type,
            @PathVariable String shopUuid,
            @Valid @RequestBody CustomerMarketplaceDTO.ShopItemsRequest request) {

        return ok(service.getShopItems(BusinessType.fromPath(type), shopUuid, request));
    }

    @PostMapping("/search")
    public ResponseEntity<ResponseData> search(
            @PathVariable String type,
            @Valid @RequestBody CustomerMarketplaceDTO.SearchRequest request) {

        return ok(service.search(BusinessType.fromPath(type), request));
    }

    private ResponseEntity<ResponseData> ok(Object data) {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(data)
                .build());
    }
}
