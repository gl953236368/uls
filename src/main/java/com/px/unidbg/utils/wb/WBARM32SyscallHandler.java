package com.px.unidbg.utils.wb;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;

/**
 * 重写unidbg 虚拟器 底层调用方法
 * 目标方法 调用so为32位 底层调用实现在 ARM32SyscallHandler
 * 继承充写 方法 进行合理化修改
 */
public class WBARM32SyscallHandler extends ARM32SyscallHandler {

    public WBARM32SyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    // 修改的点 增加时间差
    private final long nanoTime = System.nanoTime() - (1000000000L * 4800);


    protected int clock_gettime(Backend backend, Emulator<?> emulator) {
        int clk_id = backend.reg_read(66).intValue();
        Pointer tp = UnidbgPointer.register(emulator, 67);
        long offset = clk_id == 0 ? System.currentTimeMillis() * 1000000L : System.nanoTime() - this.nanoTime;
        long tv_sec = offset / 1000000000L;
        long tv_nsec = offset % 1000000000L;

        switch(clk_id) {
            case 0:
            case 1:
            case 4:
            case 6:
            case 7:
                tp.setInt(0L, (int)tv_sec);
                tp.setInt(4L, (int)tv_nsec);
                return 0;
            case 2:
            case 5:
            default:
                throw new UnsupportedOperationException("clk_id=" + clk_id);
            case 3:
                tp.setInt(0L, 0);
                tp.setInt(4L, 1);
                return 0;
        }
    }
}
