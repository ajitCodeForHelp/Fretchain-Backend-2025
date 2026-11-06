package com.bt.fretchain.primary.controller.publicapi;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.VendorDTO;
import com.bt.fretchain.primary.serviceImpl.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public (no login): a vendor / fleet owner registers itself; it starts as PENDING. */
@RestController
@RequestMapping("/public/api/vendor-registration")
@RequiredArgsConstructor
public class PublicVendorRegistrationController {

    private final VendorService service;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> register(@Valid @RequestBody VendorDTO.Register request) {
        service.register(request);
        return ResponseEntity.ok(ResponseData.builder()
                .statusCode(200).message("vendor.registration.received").build());
    }
}
