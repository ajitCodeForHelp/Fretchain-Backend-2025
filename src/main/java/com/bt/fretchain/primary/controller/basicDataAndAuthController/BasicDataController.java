package com.bt.fretchain.primary.controller.basicDataAndAuthController;

import com.bt.fretchain.primary.service.basicDataAndAuthService.BasicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/v1/api")
public class BasicDataController {

    private final BasicDataService basicDataService;
    @GetMapping(value = "/crateBasicData")
    public @ResponseBody
    String crateBasicData() {
        basicDataService.crateBasicData();
        return "Basic Data Saved Successfully.";
    }
}
