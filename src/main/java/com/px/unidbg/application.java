package com.px.unidbg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.px.unidbg"})
public class application {
    public static void main(String[] args) {
        SpringApplication.run(application.class, args);
    }
}
