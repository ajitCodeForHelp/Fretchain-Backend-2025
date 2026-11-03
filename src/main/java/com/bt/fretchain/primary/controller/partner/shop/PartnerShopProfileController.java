package com.bt.fretchain.primary.controller.partner.shop;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopPartnerDTO;
import com.bt.fretchain.primary.serviceImpl.ShopPartnerService;
import com.bt.fretchain.primary.serviceImpl.ShopRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Shop login: session info and own shop profile. */
@RestController
@RequestMapping("/partner/api/shop-profile")
@RequiredArgsConstructor
public class PartnerShopProfileController extends _BaseController {

    private final ShopPartnerService service;
    private final ShopRegistrationService registrationService;

    /** Store, role and allowed activities of the logged-in user (used to build the menu). */
    @GetMapping("/session")
    public ResponseEntity<ResponseData> session() {
        return ok(service.session(getLoggedInUser()), "record.fetched");
    }

    /** A rejected shop asks for a new review after fixing its details. */
    @PostMapping("/resubmit")
    public ResponseEntity<ResponseData> resubmit() {
        return ok(registrationService.resubmit(getLoggedInUser()), "store.resubmitted");
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseData> get() {
        return ok(service.getProfile(getLoggedInUser()), "record.fetched");
    }

    @PutMapping("/me")
    public ResponseEntity<ResponseData> update(@Valid @RequestBody ShopPartnerDTO.ProfileUpdate request) {
        return ok(service.updateProfile(getLoggedInUser(), request), "record.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
