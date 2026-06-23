package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.AdminAuthDto;
import com.bt.fretchain.primary.serviceImpl.AdminAuthServiceImpl;
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
@RequestMapping("/admin/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    protected final AdminAuthServiceImpl authService;

    @PostMapping("/login")
    public ResponseEntity<ResponseData> adminLogin(HttpServletRequest request, @Valid @RequestBody AdminAuthDto.Login login) {
        AdminAuthDto.UserDetails userDetails = authService.loginAdmin(login.getUserName(), login.getPassword());
        return new ResponseEntity<>(ResponseData.builder()
                .statusCode(200)
                .message("Login SuccessFully")
                .data(userDetails)
                .build(), HttpStatus.OK);
    }

}
