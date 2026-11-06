package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.VendorDTO;
import com.bt.fretchain.primary.serviceImpl.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin: vendors / fleet owners with their login. Permissions: VENDOR_* (PanelAuthorizationInterceptor). */
@RestController
@RequestMapping("/admin/api/vendor")
@RequiredArgsConstructor
public class VendorController extends _BaseController {

    private final VendorService service;

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@RequestBody VendorDTO.ListOrSearchData req) {
        return ok(service.list(req), "record.list.fetched");
    }

    /** key = vendor login uuid (vehicle / driver vendorUuid), value = company name. */
    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> keyValue() {
        return ok(service.keyValue(), "record.dropdown.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> get(@PathVariable String uuid) {
        return ok(service.get(uuid), "record.fetched");
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(@Valid @RequestBody VendorDTO.SaveOrUpdate req) {
        return ok(service.save(req, getLoggedInUser()), "record.saved");
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(@PathVariable String uuid, @Valid @RequestBody VendorDTO.SaveOrUpdate req) {
        return ok(service.update(uuid, req, getLoggedInUser()), "record.updated");
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> status(@PathVariable String uuid, @RequestParam Boolean active) {
        service.updateStatus(uuid, Boolean.TRUE.equals(active), getLoggedInUser());
        return ok(null, "status.updated");
    }

    /** Approve / reject / suspend (needs VENDOR_STATUS). */
    @PatchMapping("/{uuid}/approval")
    public ResponseEntity<ResponseData> approval(@PathVariable String uuid, @Valid @RequestBody VendorDTO.ApprovalUpdate req) {
        return ok(service.changeStatus(uuid, req, getLoggedInUser()), "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
