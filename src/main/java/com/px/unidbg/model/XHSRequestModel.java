package com.px.unidbg.model;

public class XHSRequestModel {
    private String deviceID;
    private String main_hmac;
    private String absoluteUrl;
    private String xyParams;
    private String xTraceid;
    private String userAgent;

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public void setMain_hmac(String main_hmac) {
        this.main_hmac = main_hmac;
    }

    public void setAbsoluteUrl(String absoluteUrl) {
        this.absoluteUrl = absoluteUrl;
    }

    public void setXyParams(String xyParams) {
        this.xyParams = xyParams;
    }

    public void setxTraceid(String xTraceid) {
        this.xTraceid = xTraceid;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public String getMain_hmac() {
        return main_hmac;
    }

    public String getAbsoluteUrl() {
        return absoluteUrl;
    }

    public String getXyParams() {
        return xyParams;
    }

    public String getxTraceid() {
        return xTraceid;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
