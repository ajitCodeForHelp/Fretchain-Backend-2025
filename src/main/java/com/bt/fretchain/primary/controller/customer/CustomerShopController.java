package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.CustomerShopDTO;
import com.bt.fretchain.primary.service.CustomerShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Customer browsing of shops.
 *
 *   GET  /customer/api/shop/categories             global category chips
 *   POST /customer/api/shop/nearby                 nearby shops (filter + sort)
 *   GET  /customer/api/shop/{shopUuid}             shop header + category tree
 *   POST /customer/api/shop/{shopUuid}/items       items of a category/subcategory (paged)
 *   POST /customer/api/shop/search                 categories + shops + items
 */
@RestController
@RequestMapping("/customer/api/shop")
@RequiredArgsConstructor
public class CustomerShopController {

    private final CustomerShopService service;

    @GetMapping("/categories")
    public ResponseEntity<ResponseData> categories() {
        return ok(service.getGlobalCategories());
    }

    @PostMapping("/nearby")
    public ResponseEntity<ResponseData> nearby(
            @Valid @RequestBody CustomerShopDTO.NearbyShopRequest request) {
        return ok(service.getNearbyShops(request));
    }

    @PostMapping("/search")
    public ResponseEntity<ResponseData> search(
            @Valid @RequestBody CustomerShopDTO.SearchRequest request) {
        return ok(service.search(request));
    }

    @GetMapping("/{shopUuid}")
    public ResponseEntity<ResponseData> shopDetail(
            @PathVariable String shopUuid,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ok(service.getShopDetail(shopUuid, latitude, longitude));
    }

    @PostMapping("/{shopUuid}/items")
    public ResponseEntity<ResponseData> shopItems(
            @PathVariable String shopUuid,
            @Valid @RequestBody CustomerShopDTO.ShopItemsRequest request) {
        return ok(service.getShopItems(shopUuid, request));
    }

    private ResponseEntity<ResponseData> ok(Object data) {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(data)
                .build());
    }
}
