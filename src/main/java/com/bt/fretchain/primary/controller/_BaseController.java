package com.bt.fretchain.primary.controller;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.security.JwtUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class _BaseController {
    @Autowired
   public JwtUserDetailsService jwtUserDetailsService;
    public SystemUser getLoggedInUser(){
        return jwtUserDetailsService.getLoggedInUser();
    }
}