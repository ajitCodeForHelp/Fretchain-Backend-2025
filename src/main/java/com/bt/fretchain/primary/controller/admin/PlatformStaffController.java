package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.PlatformStaffDTO;
import com.bt.fretchain.primary.serviceImpl.PlatformStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin: roles for platform staff (/admin/api/admin-role) and staff logins (/admin/api/admin-staff). */
@RestController
@RequiredArgsConstructor
public class PlatformStaffController extends _BaseController {

    private final PlatformStaffService service;

    // ---- roles
    @GetMapping("/admin/api/admin-role/activities")
    public ResponseEntity<ResponseData> activities() { return ok(service.activityCatalog(), "record.fetched"); }

    @GetMapping("/admin/api/admin-role/list-in-key-value")
    public ResponseEntity<ResponseData> roleKeyValue() { return ok(service.roleKeyValue(), "record.dropdown.fetched"); }

    @PostMapping("/admin/api/admin-role/list")
    public ResponseEntity<ResponseData> roles(@RequestBody PlatformStaffDTO.ListOrSearchData req) {
        return ok(service.listRoles(req), "record.list.fetched");
    }

    @GetMapping("/admin/api/admin-role/{uuid}")
    public ResponseEntity<ResponseData> role(@PathVariable String uuid) { return ok(service.getRole(uuid), "record.fetched"); }

    @PostMapping("/admin/api/admin-role/save")
    public ResponseEntity<ResponseData> saveRole(@Valid @RequestBody PlatformStaffDTO.RoleSaveOrUpdate req) {
        return ok(service.saveRole(req, getLoggedInUser()), "record.saved");
    }

    @PutMapping("/admin/api/admin-role/{uuid}")
    public ResponseEntity<ResponseData> updateRole(@PathVariable String uuid, @Valid @RequestBody PlatformStaffDTO.RoleSaveOrUpdate req) {
        return ok(service.updateRole(uuid, req, getLoggedInUser()), "record.updated");
    }

    @PatchMapping("/admin/api/admin-role/{uuid}/status")
    public ResponseEntity<ResponseData> roleStatus(@PathVariable String uuid, @RequestParam Boolean active) {
        service.updateRoleStatus(uuid, Boolean.TRUE.equals(active), getLoggedInUser());
        return ok(null, "status.updated");
    }

    // ---- staff
    @PostMapping("/admin/api/admin-staff/list")
    public ResponseEntity<ResponseData> staff(@RequestBody PlatformStaffDTO.ListOrSearchData req) {
        return ok(service.listStaff(req), "record.list.fetched");
    }

    @GetMapping("/admin/api/admin-staff/{uuid}")
    public ResponseEntity<ResponseData> staffDetail(@PathVariable String uuid) { return ok(service.getStaff(uuid), "record.fetched"); }

    @PostMapping("/admin/api/admin-staff/save")
    public ResponseEntity<ResponseData> saveStaff(@Valid @RequestBody PlatformStaffDTO.StaffSaveOrUpdate req) {
        return ok(service.saveStaff(req, getLoggedInUser()), "record.saved");
    }

    @PutMapping("/admin/api/admin-staff/{uuid}")
    public ResponseEntity<ResponseData> updateStaff(@PathVariable String uuid, @Valid @RequestBody PlatformStaffDTO.StaffSaveOrUpdate req) {
        return ok(service.updateStaff(uuid, req, getLoggedInUser()), "record.updated");
    }

    @PatchMapping("/admin/api/admin-staff/{uuid}/status")
    public ResponseEntity<ResponseData> staffStatus(@PathVariable String uuid, @RequestParam Boolean active) {
        service.updateStaffStatus(uuid, Boolean.TRUE.equals(active), getLoggedInUser());
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
