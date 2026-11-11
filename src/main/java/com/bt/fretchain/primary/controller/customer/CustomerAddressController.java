package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.CustomerAddressDTO;
import com.bt.fretchain.primary.serviceImpl.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Customer address book (login required). */
@RestController
@RequestMapping("/customer/api/address")
@RequiredArgsConstructor
public class CustomerAddressController extends _BaseController {

    private final CustomerAddressService service;

    @GetMapping("/list")
    public ResponseEntity<ResponseData> list() {
        return ok(service.list(getLoggedInUser()), "record.list.fetched");
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(@Valid @RequestBody CustomerAddressDTO.SaveOrUpdate request) {
        return ok(service.save(request, getLoggedInUser()), "record.saved");
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(@PathVariable String uuid, @Valid @RequestBody CustomerAddressDTO.SaveOrUpdate request) {
        return ok(service.update(uuid, request, getLoggedInUser()), "record.updated");
    }

    @PatchMapping("/{uuid}/default")
    public ResponseEntity<ResponseData> setDefault(@PathVariable String uuid) {
        return ok(service.setDefault(uuid, getLoggedInUser()), "record.updated");
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ResponseData> delete(@PathVariable String uuid) {
        service.delete(uuid, getLoggedInUser());
        return ok(null, "record.deleted");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
