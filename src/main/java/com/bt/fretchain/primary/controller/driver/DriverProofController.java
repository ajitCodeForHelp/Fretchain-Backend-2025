package com.bt.fretchain.primary.controller.driver;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.primary.dto.ProofDTO;
import com.bt.fretchain.primary.pojo.enums.ProofStage;
import com.bt.fretchain.primary.service.FileUploadService;
import com.bt.fretchain.primary.serviceImpl.ProofService;
import com.bt.fretchain.security.JwtUserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Proof of pickup / delivery recorded by the driver: goods check, photographs, customer e-signature.
 * Photos are uploaded one at a time to /photo; the returned link goes into the proof's photoUrls.
 */
@RestController
@RequestMapping("/driver/api/orders")
@RequiredArgsConstructor
public class DriverProofController {

    private static final long MAX_PHOTO_BYTES = 12L * 1024 * 1024;

    private final ProofService proofs;
    private final FileUploadService files;
    private final JwtUserDetailsService userDetails;

    /** stage = PICKUP or DROP; drop = 1.. for a drop (ignored for the pickup). */
    @GetMapping("/{orderUuid}/proof")
    public ResponseEntity<ResponseData> get(@PathVariable String orderUuid, @RequestParam ProofStage stage, @RequestParam(defaultValue = "0") int drop) {
        return ok(proofs.get(userDetails.getLoggedInDriver(), orderUuid, stage, drop), "record.fetched");
    }

    @PutMapping("/{orderUuid}/proof")
    public ResponseEntity<ResponseData> save(@PathVariable String orderUuid, @RequestParam ProofStage stage,
                                             @RequestParam(defaultValue = "0") int drop, @Valid @RequestBody ProofDTO.Save request) {
        return ok(proofs.save(userDetails.getLoggedInDriver(), orderUuid, stage, drop, request), "proof.saved");
    }

    @PostMapping("/photo")
    public ResponseEntity<ResponseData> photo(@RequestParam("file") MultipartFile file) {
        userDetails.getLoggedInDriver();   // drivers only
        String type = file.getContentType();
        if (file.isEmpty() || type == null || !type.toLowerCase().startsWith("image/")) throw new BadRequestException("proof.photo.invalid");
        if (file.getSize() > MAX_PHOTO_BYTES) throw new BadRequestException("proof.photo.toolarge");
        String url = files.uploadCloudinaryFile(file);
        return ok(url.replaceFirst("^http://", "https://"), "file_uploaded_successfully");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
