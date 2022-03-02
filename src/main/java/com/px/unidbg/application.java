package com.px.unidbg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@EnableAsync // 开启异步的支持
@SpringBootApplication
@ComponentScan(basePackages = {"com.px.unidbg"})
public class application {
    public static void main(String[] args) {
        SpringApplication.run(application.class, args);
    }
}
