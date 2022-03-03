package com.px.unidbg.service;

import java.util.concurrent.CompletableFuture;

public interface ZYService {
    public CompletableFuture<String> getSign(String str1, String str2);

    public String decodeAES(String hexStr);
}
