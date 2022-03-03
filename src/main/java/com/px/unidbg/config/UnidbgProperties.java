package com.px.unidbg.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data // 注在类上，提供类的get、set、equals、hashCode、canEqual、toString方法
@ConfigurationProperties(prefix = "application.unidbg")
public class UnidbgProperties {
    /**
     * unidbg 全局配置信息
     */
    // 是否使用 DynarmicFactory
    boolean dynarmic;
    // 是否打印调用信息
    boolean verbose;
    // 是否使用异步多线程
    boolean async = true;
}
