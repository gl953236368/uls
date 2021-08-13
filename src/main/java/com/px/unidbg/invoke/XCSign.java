package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class XCSign extends AbstractJni implements IOResolver {
    /**
     * Template: 携程 8.38.2.apk  -> SimpleSign()
     * Content: 涉及 lib依赖 以及 文件的检查
     * Method: unidbg补充非内置lib包 以及 对于文件环境补充(映射)
     * Algorithm: 暂无
     */
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final String dirPath = "src/main/resources/demo_resources";

    XCSign(){
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.ss.sign").build();
        // 绑定IO重定向接口
        emulator.getSyscallHandler().addIOResolver(this);
//        System.out.println("当前pid："+emulator.getPid());
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File(dirPath + "/xc/apks/xc 8-38-2.apk"));

        new AndroidModule(emulator, vm).register(memory); // 缺少libandroid.so 通过 virtualmodule来补充
        DalvikModule dm = vm.loadLibrary(new File(dirPath + "/xc/libscmain.so"), true);

        module = dm.getModule();
        vm.setJni(this);
        vm.setVerbose(false);


        dm.callJNI_OnLoad(emulator);
    }
    public String callSimpleSign(String str1, String str2){
        // 参数1字节数组 参数2固定 getdata
        List<Object> list = new ArrayList<>();
        list.add(vm.getJNIEnv());
        list.add(0);
        String input = "7be9f13e7f5426d139cb4e5dbb1fdba7";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        ByteArray byteArray = new ByteArray(vm, bytes);
        list.add(vm.addLocalObject(byteArray));
        list.add(vm.addLocalObject(new StringObject(vm, "getdata")));
        Number  number = module.callFunction(emulator, 0x869d9, list.toArray())[0];
        return vm.getObject(number.intValue()).getValue().toString();
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        // 解决启动调用 读取status/cmdline 进行文件的映射
        if(String.format("proc/%s/cmdline", emulator.getPid()).equals(pathname)){
            // 一般补包名直接把值 映射进去
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "ctrip.android.view".getBytes()));
            // 也可以创建文件 映射文件
            // return FileResult.success(new SimpleFileIO(oflags, new File(""), pathname));
        }
        if(String.format("proc/%s/status", emulator.getPid()).equals(pathname)){
            // 一般补tracepid 伪装违被调试 有可能会检测到其他的 最好是把所有的都补进去
//            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "TracerPid:\t0\n".getBytes()));
            return FileResult.success(new ByteArrayFileIO(oflags, pathname,
                    ("Name: ip.android.view\n" +
                            "State: R (running)\n" +
                            "Tgid: "+emulator.getPid()+"\n" +
                            "Pid: "+emulator.getPid()+"\n" +
                            "PPid: 3000\n" +
                            "TracerPid: 0\n" +
                            "Uid: 10163 10163 10163 10163\n" +
                            "Gid: 10163 10163 10163 10163\n" +
                            "FDSize: 512\n" +
                            "Groups: 3002 3003 9997 20163 50163\n" +
                            "VmPeak: 2319784 kB\n" +
                            "VmSize: 2240148 kB\n" +
                            "VmLck: 0 kB\n" +
                            "VmPin: 0 kB\n" +
                            "VmHWM: 413060 kB\n" +
                            "VmRSS: 310988 kB\n" +
                            "VmData: 427160 kB\n" +
                            "VmStk: 8192 kB\n" +
                            "VmExe: 20 kB\n" +
                            "VmLib: 200676 kB\n" +
                            "VmPTE: 2100 kB\n" +
                            "VmSwap: 3356 kB\n" +
                            "Threads: 149\n" +
                            "SigQ: 1/6517\n" +
                            "SigPnd: 0000000000000000\n" +
                            "ShdPnd: 0000000000000000\n" +
                            "SigBlk: 0000000000001204\n" +
                            "SigIgn: 0000000000000000\n" +
                            "SigCgt: 00000006400096fc\n" +
                            "CapInh: 0000000000000000\n" +
                            "CapPrm: 0000000000000000\n" +
                            "CapEff: 0000000000000000\n" +
                            "CapBnd: 0000000000000000\n" +
                            "CapAmb: 0000000000000000\n" +
                            "Seccomp: 2\n" +
                            "Cpus_allowed: 0f\n" +
                            "Cpus_allowed_list: 0-3\n" +
                            "Mems_allowed: 1\n" +
                            "Mems_allowed_list: 0\n" +
                            "voluntary_ctxt_switches: 6918\n" +
                            "nonvoluntary_ctxt_switches:4988").getBytes()));
        }
        return null;
    }



//    public static void main(String[] args) {
////        Logger.getLogger("com.github.unidbg.linux.ARM32SyscallHandler").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.unix.UnixSyscallHandler").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.AbstractEmulator").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm.DalvikVM").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm.BaseVM").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm").setLevel(Level.DEBUG);
//        // 可能没有加载依赖的 so，有可能undibg内也不支持该so
//        // (ARM32SyscallHandler:1906) - openat dirfd=-100, pathname=proc/27619/cmdline 系统调用 openat去打开 文件
//        XCSign sign = new XCSign();
//        // 相同参数 每次运行结果都不同 无法很好的校验
//        // 考虑 存在随机数/时间戳
//        sign.callSimpleSign();
//    }
}
