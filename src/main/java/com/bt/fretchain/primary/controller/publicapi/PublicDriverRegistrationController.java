package com.bt.fretchain.primary.controller.publicapi;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.serviceImpl.DriverRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public (no login): a driver registers; it must be verified before it can log in. */
@RestController
@RequestMapping("/public/api/driver-registration")
@RequiredArgsConstructor
public class PublicDriverRegistrationController {

    private final DriverRegistrationService service;

    /** Vendors a driver can join. Choosing none = driver of the Fretchain platform. */
    @GetMapping("/vendors")
    public ResponseEntity<ResponseData> vendors() {
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200).message("record.dropdown.fetched").data(service.vendors()).build());
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData> register(@Valid @RequestBody DriverDTO.SaveOrUpdate request) {
        service.register(request);
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200).message("driver.registration.received").build());
    }
}
