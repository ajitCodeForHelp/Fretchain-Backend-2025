package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.CustomerHomePageDTO;
import com.bt.fretchain.primary.service.CustomerHomePageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/api/home")
@RequiredArgsConstructor
public class CustomerHomePageController {

    private final CustomerHomePageService customerHomePageService;

    @GetMapping("/categories")
    public ResponseEntity<ResponseData> getCategories() {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(customerHomePageService.getCategoryList())
                        .build()
        );
    }

    @PostMapping("/vehicle-categories")
    public ResponseEntity<ResponseData> getVehicleCategories(
            @RequestBody CustomerHomePageDTO.VehicleCategoryFareRequest request
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(
                                customerHomePageService.getVehicleCategoryList(
                                        request
                                )
                        )
                        .build()
        );
    }


    @PostMapping("/check-service")
    public ResponseEntity<ResponseData> checkService(
            @Valid @RequestBody CustomerHomePageDTO.CheckServiceRequest request
    ) {

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("service.checked")
                        .data(customerHomePageService.checkService(request))
                        .build()
        );
    }
}