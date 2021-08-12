package com.px.unidbg.service.serviceImpl;

import com.px.unidbg.invoke.PddSign;
import com.px.unidbg.service.PddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PddServiceImpl implements PddService {

    @Autowired
    PddSign pddSign;

    @Override
    public String getSign() {
        synchronized (this){
            return pddSign.getInfo2();
        }
    }
}
