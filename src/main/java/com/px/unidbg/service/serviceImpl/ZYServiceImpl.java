package com.px.unidbg.service.serviceImpl;

import com.github.unidbg.worker.Worker;
import com.github.unidbg.worker.WorkerPool;
import com.github.unidbg.worker.WorkerPoolFactory;
import com.px.unidbg.config.UnidbgProperties;
import com.px.unidbg.invoke.ZYSign;
import com.px.unidbg.service.ZYService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ZYServiceImpl extends Worker implements ZYService {

    private UnidbgProperties unidbgProperties; // yml里设置的属性
    private WorkerPool workerPool; // 线程池
    private ZYSign zySign;

    public ZYServiceImpl(WorkerPool workerPool) {
        super(workerPool);

    }
    public ZYServiceImpl(Boolean dynarmic, Boolean verbose, WorkerPool workerPool) {
        // 开启动态引擎
        super(workerPool);
        this.unidbgProperties = new UnidbgProperties();
        this.unidbgProperties.setVerbose(verbose);
        this.unidbgProperties.setAsync(dynarmic);
        log.info("是否启用动态引擎:{},是否打印详细信息:{}", dynarmic, verbose);
        this.zySign = new ZYSign(this.unidbgProperties);
    }

    @Autowired
    public ZYServiceImpl(UnidbgProperties unidbgProperties,
                         @Value("${spring.task.execution.pool.core-size:4}") int poolSize) {
        super(null);
        // 初始化invoke 服务
        this.unidbgProperties = unidbgProperties;
        if(this.unidbgProperties.isAsync()){
            workerPool = WorkerPoolFactory.create((workerPool)->
                            new ZYServiceImpl(unidbgProperties.isDynarmic(), unidbgProperties.isVerbose(), workerPool),
                    Math.max(poolSize, 4));
            log.info("线程池数量：{}", Math.max(poolSize, 4));
        }else {
            // 把配置引入 invoke 对象里
            this.zySign = new ZYSign(unidbgProperties);
        }

    }

    @Async
    @Override
    public CompletableFuture<String> getSign(String str1, String str2) {
        ZYServiceImpl worker;
        String sign;

        if(unidbgProperties.isAsync()){
            while (true){
                if((worker = workerPool.borrow(2, TimeUnit.SECONDS)) == null){
                    continue;
                }
                sign = worker.doWork(str1, str2);
                workerPool.release(worker);
                break;
            }
        }else {
            synchronized (this){
                sign = this.doWork(str1, str2);
            }
        }
        return CompletableFuture.completedFuture(sign);
    }

    public String doWork(String str1, String str2) {
        return zySign.getSign(str1, str2);
    }

    @Override
    public String decodeAES(String hexStr) {
        synchronized (this){
            return zySign.getDecode(hexStr);
        }
    }

    @Override
    public void destroy() {
        try {
            zySign.destory();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Destroy: {}", zySign);
    }
}
