package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.AdminFreightOrderDTO;
import com.bt.fretchain.primary.dto.AvailableDriverResponse;
import com.bt.fretchain.primary.dto.DriverAssignmentDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.service.AdminFreightOrderService;
import com.bt.fretchain.primary.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/freight-order")
@RequiredArgsConstructor
public class AdminFreightOrderController extends _BaseController {

    private final AdminFreightOrderService adminFreightOrderService;

    private final DriverService driverService;

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody AdminFreightOrderDTO.ListRequest request
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.list.fetched")
                        .data(adminFreightOrderService.getListData(request))
                        .build()
        );
    }

    @GetMapping("/{orderUuid}")
    public ResponseEntity<ResponseData> detail(
            @PathVariable String orderUuid
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(adminFreightOrderService.getById(orderUuid))
                        .build()
        );
    }

    @PatchMapping("/{orderUuid}/status")
    public ResponseEntity<ResponseData> updateStatus(
            @PathVariable String orderUuid,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String remarks
    ) {
        SystemUser user = getLoggedInUser();

        adminFreightOrderService.updateStatus(
                orderUuid,
                status,
                remarks,
                user
        );

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("status.updated")
                        .build()
        );
    }


    @GetMapping("/{orderUuid}/available-drivers")
    public ResponseEntity<ResponseData> getAvailableDrivers(
            @PathVariable String orderUuid
    ) {
        SystemUser user = getLoggedInUser();

        List<AvailableDriverResponse> drivers =
                adminFreightOrderService.getAvailableDrivers(
                        orderUuid,
                        user
                );

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("available.drivers.fetched")
                        .data(drivers)
                        .build()
        );
    }

    /** AUTO: ask the nearest matching drivers now (needs ORDER_STATUS). */
    @PatchMapping("/{orderUuid}/auto-dispatch")
    public ResponseEntity<ResponseData> autoDispatch(@PathVariable String orderUuid) {
        int asked = adminFreightOrderService.autoDispatch(orderUuid, getLoggedInUser());
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message("status.updated").data(asked).build());
    }

    @PostMapping("/{orderUuid}/assign-driver")
    public ResponseEntity<ResponseData> assignDriver(
            @PathVariable String orderUuid,
            @Valid @RequestBody DriverAssignmentDTO.AssignRequest request
    ) {

        SystemUser user = getLoggedInUser();

        DriverAssignmentDTO.AssignResponse response =
                adminFreightOrderService.assignDriver(
                        orderUuid,
                        request,
                        user
                );

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("driver.assignment.sent")
                        .data(response)
                        .build()
        );
    }
}