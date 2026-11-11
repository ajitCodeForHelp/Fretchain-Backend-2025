package com.bt.fretchain.primary.controller.driver;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.DriverAuthDTO;
import com.bt.fretchain.primary.service.DriverAuthService;
import com.bt.fretchain.security.JwtUserDetailsService;
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

    private final JwtUserDetailsService userDetails;


    @PostMapping("/login")
    public ResponseEntity<ResponseData> driverLogin(
            @Valid @RequestBody DriverAuthDTO.Login login
    ) {

        DriverAuthDTO.AuthResponse response =
                driverAuthService.loginDriver(
                        login.getUserName(),
                        login.getPassword(),
                        login.getDevice()
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

    // ---- below: needs the driver's token ----

    @GetMapping("/me")
    public ResponseEntity<ResponseData> me() {
        return ok("OK", driverAuthService.profile(userDetails.getLoggedInDriver()));
    }

    /** Called when the FCM token is created / refreshed, or the app starts. */
    @PostMapping("/device")
    public ResponseEntity<ResponseData> device(@RequestBody DriverAuthDTO.DeviceInfo device) {
        driverAuthService.updateDevice(userDetails.getLoggedInDriver(), device);
        return ok("OK", null);
    }

    @PostMapping("/location")
    public ResponseEntity<ResponseData> location(@Valid @RequestBody DriverAuthDTO.LocationUpdate req) {
        driverAuthService.updateLocation(userDetails.getLoggedInDriver(), req.getLatitude(), req.getLongitude());
        return ok("OK", null);
    }

    @PatchMapping("/online")
    public ResponseEntity<ResponseData> online(@Valid @RequestBody DriverAuthDTO.OnlineUpdate req) {
        return ok("OK", driverAuthService.setOnline(userDetails.getLoggedInDriver(),
                Boolean.TRUE.equals(req.getOnline()), req.getLatitude(), req.getLongitude()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseData> logout() {
        driverAuthService.logout(userDetails.getLoggedInDriver());
        return ok("Logged out", null);
    }

    private ResponseEntity<ResponseData> ok(String message, Object data) {
        return new ResponseEntity<>(
                ResponseData.builder().statusCode(200).message(message).data(data).build(),
                HttpStatus.OK);
    }
}
