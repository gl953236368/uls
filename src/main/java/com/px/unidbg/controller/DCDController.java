package com.px.unidbg.controller;

import com.px.unidbg.service.DCDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("dcd")
@RestController
public class DCDController {

    @Autowired
    DCDService dcdService;

    @RequestMapping("/tkey")
    public String getTkey(){
        String t_key = dcdService.getTkey();
        return t_key;
    }

    @RequestMapping("/decrypt")
    public String getPlainText(String encrypted){
        String res = dcdService.getPlainText(encrypted);
        return res;
    }

}
