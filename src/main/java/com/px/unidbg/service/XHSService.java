package com.px.unidbg.service;

import java.util.Map;

public interface XHSService {

    public String getSign(String deviceID, String main_hmac, String absoluteUrl, Map<String, String> headerMap);
}
