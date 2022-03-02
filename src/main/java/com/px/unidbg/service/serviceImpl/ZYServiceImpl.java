package com.px.unidbg.service.serviceImpl;

import com.github.unidbg.worker.Worker;
import com.github.unidbg.worker.WorkerFactory;
import com.github.unidbg.worker.WorkerPool;
import com.github.unidbg.worker.WorkerPoolFactory;
import com.px.unidbg.config.UnidbgProperties;
import com.px.unidbg.invoke.ZYSign;
import com.px.unidbg.service.ZYService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ZYServiceImpl implements ZYService, Worker {

    private UnidbgProperties unidbgProperties;
    private WorkerPool workerPool;

    @Autowired
    private ZYSign zySign;

    public ZYServiceImpl() {

    }
    public ZYServiceImpl(Boolean dynarmic, Boolean verbose) {
        // 开启动态引擎
        this.unidbgProperties = new UnidbgProperties();
        this.unidbgProperties.setVerbose(verbose);
        this.unidbgProperties.setAsync(dynarmic);
        log.info("是否启用动态引擎:{},是否打印详细信息:{}", dynarmic, verbose);
        this.zySign = new ZYSign(this.unidbgProperties);
    }

    public ZYServiceImpl(UnidbgProperties unidbgProperties,
                         @Value("${spring.task.execution.pool.core-size:4}") int poolSize) {
        // 初始化invoke 服务
        this.unidbgProperties = unidbgProperties;
        if(this.unidbgProperties.isAsync()){
            workerPool = WorkerPoolFactory.create(()->
                            new ZYServiceImpl(unidbgProperties.isDynarmic(), unidbgProperties.isVerbose()),
                    Math.max(poolSize, 4));
            log.info("线程池数量：{}", Math.max(poolSize, 4));
        }else {
            // 把配置引入 invoke 对象里
            this.zySign = new ZYSign(unidbgProperties);
        }

    }

//    @Override
//    public String getSign(String str1, String str2) {
//        synchronized (this){
//            return zySign.getSign(str1, str2);
//        }
//    }

    @Override
    public String getSign(String str1, String str2) {
        ZYServiceImpl service;
        String sign;
        if(this.unidbgProperties.isAsync()){
            while (true){
                if((service = workerPool.borrow(2, TimeUnit.SECONDS)) == null){
                    continue;
                }
                sign = service.doWork(str1, str2);
                break;
            }
        }else {
            synchronized (this){
                return this.doWork(str1, str2);
            }
        }
        return null;
    }

    private String doWork(String str1, String str2) {
        return zySign.getSign(str1, str2);
    }

    @Override
    public String decodeAES(String hexStr) {
        synchronized (this){
            return zySign.getDecode(hexStr);
        }
    }

    @Override
    public void close() throws IOException {
        zySign.destory();
        log.info("Destroy: {}", zySign);
    }
}
