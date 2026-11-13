package com.bt.fretchain.primary.controller.driver;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.OfferDTO;
import com.bt.fretchain.primary.serviceImpl.OfferService;
import com.bt.fretchain.security.JwtUserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** The driver app: open order offers, accept / reject. */
@RestController
@RequestMapping("/driver/api/offers")
@RequiredArgsConstructor
public class DriverOfferController {

    private final OfferService offers;
    private final JwtUserDetailsService userDetails;

    /** Offers waiting for this driver's answer (not yet expired), newest first. */
    @GetMapping("/pending")
    public ResponseEntity<ResponseData> pending() {
        return ok(offers.pendingFor(userDetails.getLoggedInDriver()), "record.list.fetched");
    }

    @PostMapping("/{uuid}/accept")
    public ResponseEntity<ResponseData> accept(@PathVariable String uuid) {
        return ok(offers.accept(userDetails.getLoggedInDriver(), uuid), "offer.accepted");
    }

    @PostMapping("/{uuid}/reject")
    public ResponseEntity<ResponseData> reject(@PathVariable String uuid, @Valid @RequestBody(required = false) OfferDTO.Reject req) {
        offers.reject(userDetails.getLoggedInDriver(), uuid, req == null ? null : req.getReason());
        return ok(null, "offer.rejected");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
