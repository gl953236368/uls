package com.px.unidbg.service.serviceImpl;

import com.px.unidbg.invoke.XCSign;
import com.px.unidbg.service.XCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class XCServiceImpl implements XCService {

    @Autowired
    XCSign xcSign;

    @Override
    public String getSign(String str1, String str2) {
        return xcSign.callSimpleSign(str1, str2);
    }
}
