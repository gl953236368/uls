package com.px.unidbg.service.serviceImpl;

import com.px.unidbg.invoke.DCDSign;
import com.px.unidbg.service.DCDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DCDServiceImpl implements DCDService {

    @Autowired
    DCDSign dcdSign;

    @Override
    public String getTkey() {
        synchronized (this){
            return dcdSign.runEncrypt();
        }

    }

    @Override
    public String getPlainText(String encrypted) {
        synchronized (this){
            return dcdSign.runDecrypt(encrypted);
        }
    }
}
