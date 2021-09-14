package com.px.unidbg.controller;

import com.px.unidbg.service.PddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequestMapping("pdd")
public class PddController {

    @Autowired
    PddService pddService;

    @RequestMapping("/sign")
    public String getSign(){
        return pddService.getSign();
    }
}
