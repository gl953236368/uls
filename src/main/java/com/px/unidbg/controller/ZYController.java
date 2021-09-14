package com.px.unidbg.controller;

import com.px.unidbg.service.ZYService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RequestMapping("/zy")
//@RestController
public class ZYController {

    @Autowired
    ZYService zyService;

    @RequestMapping("/sign")
    public String getSign(){
        // 自定义参数
        String str1 = "123";
        String str2 = "abcdefg";
        return zyService.getSign(str1, str2);
    }
}
