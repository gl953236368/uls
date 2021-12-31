package com.px.unidbg.controller;

import com.px.unidbg.invoke.ZYSign;
import com.px.unidbg.service.ZYService;
import com.px.unidbg.utils.JavaMd5;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/zy")
@RestController
public class ZYController {

    @Autowired
    ZYService zyService;

    JavaMd5 md5 = new JavaMd5();

    @RequestMapping("/sign")
    public String getSign(){
        // 自定义参数
        String str1 = "123";
        String str2 = "abcdefg";
        return zyService.getSign(str1, str2);
    }

    @RequestMapping(value = "/decode", method = {RequestMethod.POST})
    public String decodeAES(@RequestBody Map param){
//        System.out.println("获取到参数："+param);
        String hexStr = (String) param.get("hexStr");
//        System.out.println(hexStr);
        return  zyService.decodeAES(hexStr);
    }

    @RequestMapping(value = "/getSign", method = {RequestMethod.POST})
    public String getMd5Sign(@RequestBody Map param){
//        System.out.println("获取到参数："+param);
        String message = (String) param.get("res");
        byte[] b1 = ZYSign.hexToBytes(message);
        String sign = md5.start(b1);
        return  sign;
    }
}
