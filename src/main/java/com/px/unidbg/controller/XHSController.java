package com.px.unidbg.controller;

import com.px.unidbg.service.XHSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/xhs")
public class XHSController {

    @Autowired
    XHSService xhsService;

    @RequestMapping("/sign")
    public String getSign(){
        String deviceID = "2767123a-be20-3e78-826b-6fe73a67873a";
        String main_hmac = "e/KPYQSKawEXKut74v5bqQpGm7f5RqkAC5P90JG7IuTIZ6/j1tSpacDPqF72nYPTkZ3K3BaBq49ZZgSqOoaGeZ8D+c4I7GpKFXue1PggxaK8xXzIVXhMnpDM+QiUQiqX";
        String absoluteUrl = "https://edith.xiaohongshu.com/api/sns/v10/search/notes?keyword=%E7%8C%AB%E7%B2%AE&filters=%5B%7B%22tags%22%3A%5B%22%E8%A7%86%E9%A2%91%E7%AC%94%E8%AE%B0%22%5D%2C%22type%22%3A%22filter_note_type%22%7D%2C%7B%22tags%22%3A%5B%22%E5%B9%BC%E7%8C%AB%22%5D%2C%22type%22%3A%22filter_hot%22%7D%5D&sort=&page=1&page_size=20&source=explore_feed&search_id=28vplsabffmisik325hxc%4028vpltilpk44isy72uccg&session_id=28vpls5i2kx38d9tspse8&api_extra=&page_pos=0&pin_note_id=&allow_rewrite=1&geo=eyJsYXRpdHVkZSI6MzkuOTk3NDA2LCJsb25naXR1ZGUiOjExNi40ODAzMjR9%0A&word_request_id=50e2bd29af5bab2c58797db38538b515&loaded_ad=&query_extra_info=&preview_ad=";
        String xyParams = "deviceId=2767123a-be20-3e78-826b-6fe73a67873a&identifier_flag=0&tz=Asia%2FShanghai&fid=1627283098102ce389ddf10e6d7d9cf8b197bbd79476&app_id=ECFAAF01&device_fingerprint1=20210407173241001d85e47664ae514042ebf05cfa49c401e24b23ecc4255b&uis=light&launch_id=1628047258&project_id=ECFAAF&device_fingerprint=20210407173241001d85e47664ae514042ebf05cfa49c401e24b23ecc4255b&versionName=6.97.0.1&platform=android&sid=session.1627283237005112246594&t=1628071973&build=6970181&x_trace_page_current=search_result_notes&lang=zh-Hans&channel=PMgdt19935737";
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("x-b3-traceid", "6868564c2f150d0f");  // 固定
        headerMap.put("xy-common-params", xyParams);
        headerMap.put("user-agent", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; Nexus 6P Build/MTC20L) Resolution/1440*2560 Version/6.97.0.1 Build/6970181 Device/(Huawei;Nexus 6P) discover/6.97.0.1 NetType/WiFi");
        return xhsService.getSign(deviceID, main_hmac, absoluteUrl,headerMap);
    }
}
