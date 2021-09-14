package com.px.unidbg.service;

import com.px.unidbg.invoke.DCDSign;
import org.springframework.beans.factory.annotation.Autowired;

public interface DCDService {

    // 获取t_key
    String getTkey();
    // 获取解密后的明文
    String getPlainText(String encrypted);
}
