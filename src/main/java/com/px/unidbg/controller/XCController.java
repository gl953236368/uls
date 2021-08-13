package com.px.unidbg.controller;

import com.px.unidbg.service.XCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/xc")
public class XCController {
    @Autowired
    XCService xcService;

    @RequestMapping("/sign")
    public String getSign(){
        String str1 = "7be9f13e7f5426d139cb4e5dbb1fdba7";
        String str2 = "getdata";
        return xcService.getSign(str1, str2);
    }
}
