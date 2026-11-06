package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.DispatchDTO;
import com.bt.fretchain.primary.pojo.enums.DeliveryService;
import com.bt.fretchain.primary.serviceImpl.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin: driver dispatch for shop / food orders. Permissions follow the Orders menu
 * (GET = ORDER_VIEW, PUT = ORDER_EDIT).
 */
@RestController
@RequestMapping("/admin/api/dispatch")
@RequiredArgsConstructor
public class DispatchController extends _BaseController {

    private final DispatchService service;

    /** Mode, radius, readiness per service and what each vehicle category serves. */
    @GetMapping("/overview")
    public ResponseEntity<ResponseData> overview() {
        return ok(service.overview(), "record.fetched");
    }

    @PutMapping("/settings")
    public ResponseEntity<ResponseData> updateSettings(@Valid @RequestBody DispatchDTO.Settings req) {
        return ok(service.updateSettings(req, getLoggedInUser()), "record.updated");
    }

    /** Eligible drivers around a point, nearest first (what staff see in MANUAL mode). */
    @GetMapping("/candidates")
    public ResponseEntity<ResponseData> candidates(
            @RequestParam DeliveryService service,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "20") int limit) {
        return ok(this.service.candidates(service, lat, lng, radiusKm, Math.max(1, Math.min(100, limit))), "record.fetched");
    }

    /** The driver AUTO mode would pick for this location. */
    @GetMapping("/auto-pick")
    public ResponseEntity<ResponseData> autoPick(
            @RequestParam DeliveryService service, @RequestParam double lat, @RequestParam double lng) {
        return ok(this.service.pick(service, lat, lng).orElse(null), "record.fetched");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
