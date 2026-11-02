package com.bt.fretchain.primary.controller.driver;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.DriverAuthDTO;
import com.bt.fretchain.primary.service.DriverAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/driver/api/auth")
@RequiredArgsConstructor
public class DriverAuthController {

    private final DriverAuthService driverAuthService;


    @PostMapping("/login")
    public ResponseEntity<ResponseData> driverLogin(
            @Valid @RequestBody DriverAuthDTO.Login login
    ) {

        DriverAuthDTO.AuthResponse response =
                driverAuthService.loginDriver(
                        login.getUserName(),
                        login.getPassword()
                );

        return new ResponseEntity<>(
                ResponseData.builder()
                        .statusCode(200)
                        .message("Login Successfully")
                        .data(response)
                        .build(),
                HttpStatus.OK
        );
    }
}