package com.bt.fretchain.primary.controller.basicDataAndAuthController;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.AuthDto;
import com.bt.fretchain.primary.service.basicDataAndAuthService.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    protected final AuthService authService;
    @PostMapping("/login")
    public ResponseEntity<ResponseData> userLogin(HttpServletRequest request, @Valid @RequestBody AuthDto.Login login) {
        AuthDto.UserDetails userDetails = authService.loginUser(login.getUserName(), login.getPassword());
        return new ResponseEntity<>(ResponseData.builder()
                .statusCode(200)
                .message("Login SuccessFully")
                .data(userDetails)
                .build(), HttpStatus.OK);
    }

}
