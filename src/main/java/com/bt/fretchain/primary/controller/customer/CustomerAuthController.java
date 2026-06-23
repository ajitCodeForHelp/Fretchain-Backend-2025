package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.CustomerAuthDTO;
import com.bt.fretchain.primary.service.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/api/auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/send-otp")
    public ResponseEntity<ResponseData> sendOtp(
            @Valid @RequestBody CustomerAuthDTO.SendOtp request
    ) {
        customerAuthService.sendOtp(request);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("otp.sent")
                        .build()
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseData> verifyOtp(
            @Valid @RequestBody CustomerAuthDTO.VerifyOtp request
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("login.success")
                        .data(customerAuthService.verifyOtp(request))
                        .build()
        );
    }
}