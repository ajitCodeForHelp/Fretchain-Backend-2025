package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.CustomerRestaurantDTO;
import com.bt.fretchain.primary.service.CustomerRestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Customer browsing of restaurants.
 *
 *   GET  /customer/api/restaurant/cuisines                     cuisine chips
 *   POST /customer/api/restaurant/nearby                       nearby restaurants (filter + sort)
 *   GET  /customer/api/restaurant/{restaurantUuid}             header + menu sections
 *   GET  /customer/api/restaurant/{restaurantUuid}/menu        header + full menu with dishes
 *   POST /customer/api/restaurant/{restaurantUuid}/dishes      dishes of a section (paged)
 *   POST /customer/api/restaurant/search                       cuisines + restaurants + dishes
 */
@RestController
@RequestMapping("/customer/api/restaurant")
@RequiredArgsConstructor
public class CustomerRestaurantController {

    private final CustomerRestaurantService service;

    @GetMapping("/cuisines")
    public ResponseEntity<ResponseData> cuisines() {
        return ok(service.getCuisines());
    }

    @PostMapping("/nearby")
    public ResponseEntity<ResponseData> nearby(
            @Valid @RequestBody CustomerRestaurantDTO.NearbyRestaurantRequest request) {
        return ok(service.getNearbyRestaurants(request));
    }

    @PostMapping("/search")
    public ResponseEntity<ResponseData> search(
            @Valid @RequestBody CustomerRestaurantDTO.SearchRequest request) {
        return ok(service.search(request));
    }

    @GetMapping("/{restaurantUuid}")
    public ResponseEntity<ResponseData> detail(
            @PathVariable String restaurantUuid,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ok(service.getRestaurantDetail(restaurantUuid, latitude, longitude));
    }

    @GetMapping("/{restaurantUuid}/menu")
    public ResponseEntity<ResponseData> menu(
            @PathVariable String restaurantUuid,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ok(service.getRestaurantMenu(restaurantUuid, latitude, longitude));
    }

    @PostMapping("/{restaurantUuid}/dishes")
    public ResponseEntity<ResponseData> dishes(
            @PathVariable String restaurantUuid,
            @Valid @RequestBody CustomerRestaurantDTO.DishRequest request) {
        return ok(service.getDishes(restaurantUuid, request));
    }

    private ResponseEntity<ResponseData> ok(Object data) {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200)
                .message("record.fetched")
                .data(data)
                .build());
    }
}
