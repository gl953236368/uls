package com.px.unidbg;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@EnableAsync // 开启异步的支持
@SpringBootApplication
@ConfigurationPropertiesScan
public class application {
    private static final String SERVER_PORT = "server.port";
    private static final String SERVER_SERVLET_CONTEXT_PATH = "server.servlet.context-path";
    private static final String SPRING_APPLICATION_NAME = "spring.application.name";
    private static final String DEFAULT_APPLICATION_NAME = "unidbg-local-server";
    private static final String PROFILE_PREFIX = "application";

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(application.class);
        Environment environment = springApplication.run(args).getEnvironment();
        logApplicationStartup(environment);
    }

    private static void logApplicationStartup(Environment env) {
        String serverPort = env.getProperty(SERVER_PORT);
        String contextPath = env.getProperty(SERVER_SERVLET_CONTEXT_PATH);
        if (StringUtils.isBlank(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = InetAddress.getLoopbackAddress().getHostAddress();
        List<String> profiles = new ArrayList<>(env.getActiveProfiles().length + 1);
        profiles.add(PROFILE_PREFIX);
        for (String profile : env.getActiveProfiles()) {
            profiles.add(PROFILE_PREFIX + "-" + profile);
        }
        log.info("\n----------------------------------------------------------\n\t"
                        + "应用: \t\t{} 已启动!\n\t"
                        + "地址: \t\thttp://{}:{}{}\n\t"
                        + "演示访问: \thttp://{}:{}{}zy/sign (浏览器直接打开)\n\t"
                        + "项目结构参考: \thttps://github.com/anjia0532/unidbg-boot-server\n\t"
                        + "配置文件: \t{}\n----------------------------------------------------------",
                StringUtils.defaultIfBlank(env.getProperty(SPRING_APPLICATION_NAME), DEFAULT_APPLICATION_NAME),
                hostAddress,
                serverPort,
                contextPath,
                hostAddress,
                serverPort,
                contextPath,
                profiles
                );
        log.info("\n----------------------------------------------------------\n");
    }

}
