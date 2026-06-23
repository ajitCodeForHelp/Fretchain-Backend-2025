package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.CategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/category")
@RequiredArgsConstructor
public class CategoryController extends _BaseController {

    private final CategoryService categoryService;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody CategoryDTO.SaveOrUpdate request
    ) {

        SystemUser user = getLoggedInUser();

        CategoryDTO.Detail data =
                categoryService.save(request, user);

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
            @Valid @RequestBody CategoryDTO.SaveOrUpdate request
    ) {

        SystemUser user = getLoggedInUser();

        CategoryDTO.Detail data =
                categoryService.update(
                        uuid,
                        request,
                        user
                );

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

        CategoryDTO.Detail data =
                categoryService.getById(uuid);

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
            @Valid @RequestBody CategoryDTO.ListOrSearchData request
    ) {

        DataTableResponsePacket<CategoryDTO.Detail> data =
                categoryService.getListData(request);

        return ResponseEntity.ok(
                ResponseData
                        .builder()
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

        categoryService.updateStatus(
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

    @GetMapping("/list-in-key-value")
    public ResponseEntity<ResponseData> listInKeyValue(@RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {

        List<KeyValueDto> data =
                categoryService.getKeyValueList(lang);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.dropdown.fetched")
                        .data(data)
                        .build()
        );
    }
}