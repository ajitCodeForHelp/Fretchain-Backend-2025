package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.SubCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.service.SubCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/sub-category")
@RequiredArgsConstructor
public class SubCategoryController
        extends _BaseController {

    private final SubCategoryService subCategoryService;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody
            SubCategoryDTO.SaveOrUpdate request
    ) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.saved")
                        .data(
                                subCategoryService.save(
                                        request,
                                        user
                                )
                        )
                        .build()
        );
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody
            SubCategoryDTO.SaveOrUpdate request
    ) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.updated")
                        .data(
                                subCategoryService.update(
                                        uuid,
                                        request,
                                        user
                                )
                        )
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
                        .data(
                                subCategoryService.getById(uuid)
                        )
                        .build()
        );
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @RequestBody
            SubCategoryDTO.ListOrSearchData request
    ) {

        DataTableResponsePacket<SubCategoryDTO.Detail>
                data =
                subCategoryService.getListData(request);

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

        subCategoryService.updateStatus(
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
                subCategoryService.getKeyValueList(lang);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.dropdown.fetched")
                        .data(data)
                        .build()
        );
    }
}