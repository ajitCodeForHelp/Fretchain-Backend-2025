package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.VehicleCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.service.VehicleCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/vehicle-category")
@RequiredArgsConstructor
public class VehicleCategoryController extends _BaseController {

    private final VehicleCategoryService vehicleCategoryService;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody VehicleCategoryDTO.SaveOrUpdate request
    ) {
        SystemUser user = getLoggedInUser();

        VehicleCategoryDTO.Detail data = vehicleCategoryService.save(request, user);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.saved")
                        .data(data)
                        .build()
        );
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody VehicleCategoryDTO.SaveOrUpdate request
    ) {
        SystemUser user = getLoggedInUser();

        VehicleCategoryDTO.Detail data =
                vehicleCategoryService.update(uuid, request, user);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.updated")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> getById(
            @PathVariable String uuid
    ) {
        VehicleCategoryDTO.Detail data = vehicleCategoryService.getById(uuid);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody VehicleCategoryDTO.ListOrSearchData request
    ) {
        DataTableResponsePacket<VehicleCategoryDTO.Detail> data =
                vehicleCategoryService.getListData(request);

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

        vehicleCategoryService.updateStatus(uuid, active, user);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("status.updated")
                        .build()
        );
    }

    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> listInKeyValue(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang
    ) {
        List<KeyValueDto> data = vehicleCategoryService.getKeyValueList(lang);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.dropdown.fetched")
                        .data(data)
                        .build()
        );
    }
}