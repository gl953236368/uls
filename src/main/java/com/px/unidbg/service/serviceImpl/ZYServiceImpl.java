package com.px.unidbg.service.serviceImpl;

import com.px.unidbg.invoke.ZYSign;
import com.px.unidbg.service.ZYService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//@Service
public class ZYServiceImpl implements ZYService {

    @Autowired
    ZYSign zySign;

    @Override
    public String getSign(String str1, String str2) {
        synchronized (this){
            return zySign.getSign(str1, str2);
        }
    }
}
