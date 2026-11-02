package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ServiceRadiusDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.service.ServiceRadiusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/service-radius")
@RequiredArgsConstructor
public class ServiceRadiusController extends _BaseController {

    private final ServiceRadiusService serviceRadiusService;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody ServiceRadiusDTO.SaveOrUpdate request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.saved")
                        .data(serviceRadiusService.save(request, user))
                        .build()
        );
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody ServiceRadiusDTO.SaveOrUpdate request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.updated")
                        .data(serviceRadiusService.update(uuid, request, user))
                        .build()
        );
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> getById(
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(serviceRadiusService.getById(uuid))
                        .build()
        );
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody ServiceRadiusDTO.ListOrSearchData request
    ) {
        DataTableResponsePacket<ServiceRadiusDTO.Detail> data =
                serviceRadiusService.getListData(request);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.list.fetched")
                        .data(data)
                        .build()
        );
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> updateStatus(
            @PathVariable String uuid,
            @RequestParam Boolean active
    ) {
        SystemUser user = getLoggedInUser();

        serviceRadiusService.updateStatus(
                uuid,
                active,
                user
        );

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("status.updated")
                        .build()
        );
    }
}