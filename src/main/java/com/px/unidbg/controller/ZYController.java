package com.px.unidbg.controller;

import com.px.unidbg.config.UnidbgProperties;
import com.px.unidbg.invoke.ZYSign;
import com.px.unidbg.service.ZYService;
import com.px.unidbg.utils.JavaMd5;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RequestMapping("/zy")
@RestController
public class ZYController {

    @Autowired
    ZYService zyService;

    JavaMd5 md5 = new JavaMd5();

    @SneakyThrows // 利用泛型将抛出异常
    @RequestMapping("/sign")
    public String getSign(){
        // 自定义参数
        String str1 = "123";
        String str2 = "abcdefg";
        String res = zyService.getSign(str1, str2).get();
        log.info("调用成功结果为: {}",res);
        return res;
    }

    @RequestMapping(value = "/getSign", method = {RequestMethod.POST})
    public String getMd5Sign(@RequestBody Map param){
        String message = (String) param.get("res");
        byte[] b1 = ZYSign.hexToBytes(message);
        String sign = md5.start(b1);
        return  sign;
    }
}
