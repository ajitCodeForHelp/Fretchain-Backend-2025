package com.bt.fretchain.primary.controller.partner.vendor;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.dto.VehicleDTO;
import com.bt.fretchain.primary.dto.VendorDTO;
import com.bt.fretchain.primary.serviceImpl.VendorFleetService;
import com.bt.fretchain.primary.serviceImpl.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Vendor (fleet owner) panel APIs. Only VENDOR logins reach /partner/api/vendor-* (PanelAuthorizationInterceptor);
 * the vendor is always the logged-in one.
 */
@RestController
@RequiredArgsConstructor
public class VendorPanelController extends _BaseController {

    private final VendorService vendorService;
    private final VendorFleetService fleet;

    // ---- company profile
    @GetMapping("/partner/api/vendor-profile/me")
    public ResponseEntity<ResponseData> me() {
        return ok(vendorService.me(getLoggedInUser()), "record.fetched");
    }

    @PutMapping("/partner/api/vendor-profile/me")
    public ResponseEntity<ResponseData> updateMe(@Valid @RequestBody VendorDTO.Profile req) {
        return ok(vendorService.updateMe(getLoggedInUser(), req), "record.updated");
    }

    @PostMapping("/partner/api/vendor-profile/change-password")
    public ResponseEntity<ResponseData> changePassword(@Valid @RequestBody VendorDTO.ChangePassword req) {
        vendorService.changePassword(getLoggedInUser(), req);
        return ok(null, "password.changed");
    }

    // ---- vehicles
    @PostMapping("/partner/api/vendor-vehicle/list")
    public ResponseEntity<ResponseData> vehicles(@RequestBody VehicleDTO.ListOrSearchData req) {
        return ok(fleet.vehicles(getLoggedInUser(), req), "record.list.fetched");
    }

    @GetMapping("/partner/api/vendor-vehicle/list-in-key-value")
    public ResponseEntity<ResponseData> vehicleKeyValue() {
        return ok(fleet.vehicleKeyValue(getLoggedInUser()), "record.dropdown.fetched");
    }

    @GetMapping("/partner/api/vendor-vehicle/categories")
    public ResponseEntity<ResponseData> vehicleCategories(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
        return ok(fleet.vehicleCategories(getLoggedInUser(), lang), "record.dropdown.fetched");
    }

    @GetMapping("/partner/api/vendor-vehicle/{uuid}")
    public ResponseEntity<ResponseData> vehicle(@PathVariable String uuid) {
        return ok(fleet.vehicle(getLoggedInUser(), uuid), "record.fetched");
    }

    @PostMapping("/partner/api/vendor-vehicle/save")
    public ResponseEntity<ResponseData> saveVehicle(@Valid @RequestBody VehicleDTO.SaveOrUpdate req) {
        return ok(fleet.saveVehicle(getLoggedInUser(), req), "record.saved");
    }

    @PutMapping("/partner/api/vendor-vehicle/{uuid}")
    public ResponseEntity<ResponseData> updateVehicle(@PathVariable String uuid, @Valid @RequestBody VehicleDTO.SaveOrUpdate req) {
        return ok(fleet.updateVehicle(getLoggedInUser(), uuid, req), "record.updated");
    }

    @PatchMapping("/partner/api/vendor-vehicle/{uuid}/status")
    public ResponseEntity<ResponseData> vehicleStatus(@PathVariable String uuid, @RequestParam Boolean active) {
        fleet.vehicleStatus(getLoggedInUser(), uuid, Boolean.TRUE.equals(active));
        return ok(null, "status.updated");
    }

    // ---- drivers
    @PostMapping("/partner/api/vendor-driver/list")
    public ResponseEntity<ResponseData> drivers(@RequestBody DriverDTO.ListOrSearchData req) {
        return ok(fleet.drivers(getLoggedInUser(), req), "record.list.fetched");
    }

    @GetMapping("/partner/api/vendor-driver/list-in-key-value")
    public ResponseEntity<ResponseData> driverKeyValue() {
        return ok(fleet.driverKeyValue(getLoggedInUser()), "record.dropdown.fetched");
    }

    @GetMapping("/partner/api/vendor-driver/{uuid}")
    public ResponseEntity<ResponseData> driver(@PathVariable String uuid) {
        return ok(fleet.driver(getLoggedInUser(), uuid), "record.fetched");
    }

    @PostMapping("/partner/api/vendor-driver/save")
    public ResponseEntity<ResponseData> saveDriver(@Valid @RequestBody DriverDTO.SaveOrUpdate req) {
        return ok(fleet.saveDriver(getLoggedInUser(), req), "record.saved");
    }

    @PutMapping("/partner/api/vendor-driver/{uuid}")
    public ResponseEntity<ResponseData> updateDriver(@PathVariable String uuid, @Valid @RequestBody DriverDTO.SaveOrUpdate req) {
        return ok(fleet.updateDriver(getLoggedInUser(), uuid, req), "record.updated");
    }

    @PatchMapping("/partner/api/vendor-driver/{uuid}/status")
    public ResponseEntity<ResponseData> driverStatus(@PathVariable String uuid, @RequestParam Boolean active) {
        fleet.driverStatus(getLoggedInUser(), uuid, Boolean.TRUE.equals(active));
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
