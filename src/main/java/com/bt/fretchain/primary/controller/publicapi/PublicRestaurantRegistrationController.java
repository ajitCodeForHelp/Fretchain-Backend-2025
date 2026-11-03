package com.bt.fretchain.primary.controller.publicapi;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.RestaurantRegistrationDTO;
import com.bt.fretchain.primary.serviceImpl.RestaurantRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public (no login): a restaurant registers itself; it starts as PENDING. */
@RestController
@RequestMapping("/public/api/restaurant-registration")
@RequiredArgsConstructor
public class PublicRestaurantRegistrationController {

    private final RestaurantRegistrationService service;

    @GetMapping("/global-categories")
    public ResponseEntity<ResponseData> globalCategories() {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200).message("record.dropdown.fetched").data(service.globalCategories()).build());
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData> register(@Valid @RequestBody RestaurantRegistrationDTO.Register request) {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200).message("registration.received").data(service.register(request)).build());
    }
}
