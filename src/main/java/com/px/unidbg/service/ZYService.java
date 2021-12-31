package com.px.unidbg.service;

import org.springframework.stereotype.Service;

public interface ZYService {
    public String getSign(String str1, String str2);

    String decodeAES(String hexStr);
}
