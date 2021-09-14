package com.px.unidbg.service.serviceImpl;

import com.px.unidbg.invoke.XHSSign;
import com.px.unidbg.service.XHSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

//@Service
public class XHSServiceImpl implements XHSService {

    @Autowired
    XHSSign xhsSign;

    @Override
    public String getSign(String deviceID, String main_hmac, String absoluteUrl, Map<String, String> headerMap) {
        synchronized (this){
            return xhsSign.main(deviceID, main_hmac, absoluteUrl, headerMap);
        }
    }
}
