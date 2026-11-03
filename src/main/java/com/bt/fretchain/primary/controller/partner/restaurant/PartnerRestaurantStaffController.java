package com.bt.fretchain.primary.controller.partner.restaurant;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.RestaurantPartnerDTO;
import com.bt.fretchain.primary.serviceImpl.RestaurantPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Restaurant login: staff logins of the restaurant and their roles. */
@RestController
@RequestMapping("/partner/api/restaurant-staff")
@RequiredArgsConstructor
public class PartnerRestaurantStaffController extends _BaseController {

    private final RestaurantPartnerService service;

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@RequestBody RestaurantPartnerDTO.ListOrSearchData request) {
        return ok(service.listStaff(getLoggedInUser(), request), "record.list.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> get(@PathVariable String uuid) {
        return ok(service.getStaff(getLoggedInUser(), uuid), "record.fetched");
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(@Valid @RequestBody RestaurantPartnerDTO.StaffSaveOrUpdate request) {
        return ok(service.saveStaff(getLoggedInUser(), request), "record.saved");
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(@PathVariable String uuid,
                                               @Valid @RequestBody RestaurantPartnerDTO.StaffSaveOrUpdate request) {
        return ok(service.updateStaff(getLoggedInUser(), uuid, request), "record.updated");
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> status(@PathVariable String uuid, @RequestParam Boolean active) {
        service.updateStaffStatus(getLoggedInUser(), uuid, Boolean.TRUE.equals(active));
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
