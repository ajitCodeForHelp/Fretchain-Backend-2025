package com.bt.fretchain.primary.controller;

import com.bt.cloudLibrary.exception.BadRequestException;
import com.bt.cloudLibrary.messaging.EmailComponent;
import com.bt.fretchain.annotation.TranslateResponseMessage;
import com.bt.fretchain.bean.ResponsePacket;
import com.bt.fretchain.utils.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UrlController {

    @Autowired
    EmailComponent emailComponent;
    @TranslateResponseMessage
    @GetMapping(value = "/pages/{pageName}")
    public ResponseEntity<ResponsePacket> pages(@PathVariable String pageName) throws BadRequestException {
        String htmlContent = "";
        if (pageName.equalsIgnoreCase("about-us")) {
            htmlContent = Const.WebSiteCMS.AboutUs;
        } else if (pageName.equalsIgnoreCase("terms-conditions")) {
            htmlContent = Const.WebSiteCMS.TermsAndCondition;
        } else if (pageName.equalsIgnoreCase("privacy-policy")) {
            htmlContent = Const.WebSiteCMS.PrivacyPolicy;
        }
        else if (pageName.equalsIgnoreCase("return-replacement")) {
            htmlContent = Const.WebSiteCMS.ReturnAndReplacement;
        }
        else if (pageName.equalsIgnoreCase("contact")) {
            htmlContent = Const.WebSiteCMS.ContactUs;
        }
        else if (pageName.equalsIgnoreCase("refund-policy")) {
            htmlContent = Const.WebSiteCMS.RefundPolicy;
        }
        else if (pageName.equalsIgnoreCase("trackOrder")) {
            htmlContent = Const.WebSiteCMS.TrackOrder;
        }
        else if (pageName.equalsIgnoreCase("Shipping-Policy")) {
            htmlContent = Const.WebSiteCMS.ShippingPolicy;
        }
        else if (pageName.equalsIgnoreCase("Cancellation-Policy")) {
            htmlContent = Const.WebSiteCMS.CancellationPolicy;
        }
        return new ResponseEntity<>(ResponsePacket.builder()
                .errorCode(0)
                .responsePacket(htmlContent)
                .message("Page Data.")
                .build(), HttpStatus.OK);
    }



}
