package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.*;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.px.unidbg.config.UnidbgProperties;
import com.px.unidbg.utils.wb.WBARM32SyscallHandler;
import com.px.unidbg.utils.wb.WBAndroidModule;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class WBWIND extends AbstractJni implements IOResolver {
    /***
     * Template: weibo 11.0.0.apk  -> deal 环境信息收集 模拟
     * Content: unidbg底层实现函数的替换、必要依赖补充、必要文件补充
     * Method:
     *      1. 自定义ARM32SystemHandle、AndroidModule完善，满足自己样本需求的底层实现函数
     *      2. 调用SystemPropertyHook，对于apk的所调用的system_property_get 进行捕获和补充
     *      3. 借用Debugger断点，结合ida中出现频繁的可疑的导入函数、过滤可能出现问题的函数进行断点观察，
     *          针对具体情况修改unidbg的底层实现或者patch代码
     *      4. 文件重定向，活用断点，追溯上下文，结合上下文伪造访问文件
     *      5. hook patch那些不好补的数据以及监控数据生成是否符合预期
     * Algorithm: 暂无
     */
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final Boolean isVerbose;
    private final String dirPath = "src/main/resources/demo_resources";
    private final DvmClass cNative;


    public WBWIND(UnidbgProperties unidbgProperties){
        isVerbose = unidbgProperties.isVerbose();
        AndroidEmulatorBuilder builder = new AndroidEmulatorBuilder(false){
            // 用自己的handler 替代unidbg的Arm32SyscallHandler
            @Override
            public AndroidEmulator build() {
                return new AndroidARMEmulator(processName, rootDir, backendFactories){
                    @Override
                    protected UnixSyscallHandler<AndroidFileIO> createSyscallHandler(SvcMemory svcMemory) {
                        return new WBARM32SyscallHandler(svcMemory);
                    }
                };
            }
        };

        emulator = builder.build(); // 创建模拟器实例
        emulator.getSyscallHandler().setEnableThreadDispatcher(true); // 分发器
        final Memory memory = emulator.getMemory(); // 模拟器内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        // 创建Android虚拟机
        vm = emulator.createDalvikVM(new File(dirPath + "/wb/apks/wb.apk"));
        vm.setVerbose(isVerbose);
        vm.setJni(this);

        emulator.getSyscallHandler().addIOResolver(this); // 开启文件重定向

        new WBAndroidModule(emulator, vm).register(memory); // 自定义虚拟libandroid.so 解决部分依赖问题

        memory.addHookListener(hookSystemProperty()); // 开启对 system_property_get的hook

        DalvikModule dm = vm.loadLibrary(new File(dirPath + "/wb/libwind.so"), true);
        module = dm.getModule(); // 获得libwind 句柄
        hookLib(); // 结合ida 针对可能调用到的部分底层函数的调用 进行hook
        cNative = vm.resolveClass("com/weibo/ssosdk/LibHelper");
        dm.callJNI_OnLoad(emulator);
    }

    /**
     * hook system_property_get 属性的调用
     * 配合真鸡属性进行填充
     * @return
     */
    public SystemPropertyHook  hookSystemProperty(){
        SystemPropertyHook systemPropertyHook = new SystemPropertyHook(emulator); // 对system_property_get的Hook操作
        systemPropertyHook.setPropertyProvider(new SystemPropertyProvider() {
            @Override
            public String getProperty(String s) {
                System.out.println("SO call system_property_get: " + s);
                switch (s){
                    case "ro.build.version.sdk":{
                        // sub_4ad4 判断 sdk情况
                        return "29";
                    }
                    case "ro.serialno":{
                        return "f8a995f5";
                    }
                    case "ro.product.brand":{
                        return "Xiaomi";
                    }
                    case "ro.product.model":{
                        return "MIX 2S";
                    }
                    case "ro.product.manufacturer":{
                        return "Xiaomi";
                    }
                    case "ro.build.product":{
                        return "polaris";
                    }
                    case "ro.product.cpu.abi":{
                        return "arm64-v8a";
                    }
                    case "ro.product.cpu.abilist":{
                        return "arm64-v8a,armeabi-v7a,armeabi";
                    }
                    case "ro.product.device":{
                        return "polaris";
                    }
                    case "ro.build.id":{
                        return "QKQ1.190828.002";
                    }
                    case "ro.build.fingerprint":{
                        return "Xiaomi/polaris/polaris:10/QKQ1.190828.002/V12.0.2.0.QDGCNXM:user/release-keys";
                    }
                    case "ro.build.host":{
                        return "c3-miui-ota-bd134.bj";
                    }
                    case "ro.build.tags":{
                        return "release-keys";
                    }
                    case "ro.build.date.utc":{
                        return "1604422370";
                    }
                    case "build_type":{
                        return "user";
                    }
                    case "ro.build.user":{
                        return "builder";
                    }
                    case "ro.build.version.release":{
                        return "10";
                    }
                    case "ro.build.version.incremental":{
                        return "V12.0.2.0.QDGCNXM";
                    }
                    case "ro.product.board":{
                        return "sdm845";
                    }
                    case "ro.bootloader":{
                        return "unknown";
                    }
                    case "ro.debuggable":{
                        // 为1时即全局可调试
                        return "0";
                    }
                    case "ro.secure":{
                        // 为0时即root
                        return "1";
                    }
                    case "ro.opengles.version":{
                        return "196610";
                    }
                    case "init.svc.adbd":{
                        return "stopped";
                    }
                    case "ro.build.display.id":{
                        return "QQ3A.200805.001";
                    }
                    case "ro.hardware":{
                        return "blueline";
                    }
                }
                return "";
            }
        });
        return systemPropertyHook;
    }


    /**
     * 主动 Hook SO 导入函数中一些常常出现问题的库函数以便在其出错时能及时反应
     * 针对ida部分导入函数 过滤可能出现问题的函数
     *  dlopen/dlsym/clock_gettime/sigaction/socket/popen/uname/stat/statfs/
     *  getpid/syscall/dladdr/system_property_get/getrusage/sysinfo
     *
     */
    public void hookLib(){
        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module.findSymbolByName("dlopen").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call dlopen");
                return false;
            }
        });

        debugger.addBreakPoint(module.findSymbolByName("dlsym").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call dlsym");
                return false;
            }
        });

        // clock_gettime 时间采集，断点追溯。 捕获开机到至今的毫秒数
        // Unidbg 对此实现于 ARM32SyscallHandler 类（分析的为32位so）
        //  ————————近似于Unidbg模拟器运行环境的开机时间， 与真实情况有出入。
//        debugger.addBreakPoint(module.findSymbolByName("clock_gettime").getAddress(), new BreakPointCallback() {
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                System.out.println("So call clock_gettime");
//                return false;
//            }
//        });

        debugger.addBreakPoint(module.findSymbolByName("sigaction").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call sigaction");
                return false;
            }
        });

        debugger.addBreakPoint(module.findSymbolByName("socket").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call socket");
                return false;
            }
        });

        debugger.addBreakPoint(module.findSymbolByName("popen").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call popen");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("uname").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call uname");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("stat").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call stat");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("statfs").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call statfs");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("getpid").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call getpid");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("syscall").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call syscall");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("dladdr").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call dladdr");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("getrusage").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call getrusage");
                return false;
            }
        });


        debugger.addBreakPoint(module.findSymbolByName("sysinfo").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("So call sysinfo");
                return false;
            }
        });
    }

    /**
     * 对部分疑点 进行hook观察 以及 patch
     */
    public void myHook(){
        Debugger debugger = emulator.attach();

        // 1. 断点 0x7C02 为 在主要收集函数中 clock_gettime 的返回值的获取位置
        //    即 movs r2,r0 处,r0 为 "开机至此的毫秒数", 数据和现实的不太符合，对 unidbg底层的arm32syscallHandler 这个方法进行重写
//        debugger.addBreakPoint(module.base + 0x7c02);

        // 2. 根据主函数（sub_7bc8）收集情况 hook修改对于mac地址的获取（具体方法实现为 sub_4164）
        MemoryBlock v3Block = emulator.getMemory().malloc(4, false);
        MemoryBlock macBlock = emulator.getMemory().malloc(0x1000, false);
        final UnidbgPointer v3 = v3Block.getPointer();
        final UnidbgPointer macPtr = macBlock.getPointer();
        debugger.addBreakPoint(module, 0x4164, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("不执行Sub_4164，直接返回符合期待的字符串");
                RegisterContext registerContext = emulator.getContext();
                v3.setPointer(0, macPtr);
                macPtr.setString(0, "[{\"name\":\"bond0\",\"mac\":\"F6:64:A7:9B:C9:F9\"},{\"name\":\"dummy0\",\"mac\":\"52:DC:8E:60:2E:A6\"},{\"name\":\"wlan0\",\"mac\":\"F4:60:E2:96:DB:64\"},{\"name\":\"wlan1\",\"mac\":\"F4:60:E2:17:DB:64\"},{\"name\":\"p2p0\",\"mac\":\"F6:60:E2:18:DB:64\"}]");
                // 重写R0的结果 以及 PC寄存器的指向
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, v3.peer);
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, registerContext.getLRPointer().peer);
                return true;
            }
        });

        // 3. open /proc/version追溯 在报错前断下
        //   bt追溯 进入 发现进入主函数（sub_7bc8）采集内核信息 补充对应文件
//        emulator.attach().addBreakPoint(module.base + 0x04f61);

        // 4.  对TLB_time 进行hook replace （实现主要在sub_3290中）
        //      1. frida hook 目标结果，双精度如何处理
        //      2. unidbg replace 替换目标函数


        // 5. popen函数处理
        //    popen 函数：调用fork（）产生子进程，然后从子进程中调用/bin/sh -c 来执行参数command的指令（在进程中执行一个shell命令）
        //    popen 调用捕获，popen底层依赖于系统调用pipe传输数据，可自定义syscallhandler，并根据popen的入参传入合适的值
        //          popen 返回一个file指针，fopen返回也是file指针。样本调用popen时，可以返回由fopen得到的file指针，
        //          这样就可以把补系统调用的任务转变成补文件访问的操作，把补系统调用的任务转变成补文件访问的操作
        debugger.addBreakPoint(module.findSymbolByName("popen").getAddress(), new BreakPointCallback() {
            // 模拟调用fopen ps 指令 并获得file 指针，给popen一个file指针 伪造出popen调用命令的操作
            final UnidbgPointer psptr = UnidbgPointer.pointer(emulator, module.findSymbolByName("fopen").call(emulator, "ps", "r"));
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("SO call popen && onHit");
                RegisterContext registerContext = emulator.getContext();
                String cmd = registerContext.getPointerArg(0).getString(0);
                System.out.println("popen arg: " + cmd);
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, registerContext.getLRPointer().peer);
                if(cmd.equals("ps")){
                    emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, psptr.peer);
                }
                return false;
            }
        });

        // 6. 对最终结果收集完 数据返回值进行打印 （sub_8DC0中）
        //    后续对于结果的处理 sub96e4 中 aes + base64
        //    mr0 0x900 多打印一点
//        debugger.addBreakPoint(module.base + 0x8f06);

    }


    /**
     * 对捕获的 读文件操作进行 重定向
     * @param emulator
     * @param pathname
     * @param oflags
     * @return
     */
    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("SO open:" + pathname);
        if("/proc/self/maps".equals(pathname)){ // 访问maps时下断点
//            emulator.attach().debug();
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath + "/wb/file/fakemaps.txt"), pathname));
        }else if("/proc/version".equals(pathname)){
//            emulator.attach().debug();
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath + "/wb/file/version"), pathname));
        }else if("/proc/cpuinfo".equals(pathname)){
//            emulator.getUnwinder().unwind(); // 断点并打印堆栈
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath + "/wb/file/cpuinfo"), pathname));
        }else if("/sys/devices/system/cpu/present".equals(pathname)){ // 已经被系统识别的cpu
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "0-7".getBytes(StandardCharsets.UTF_8)));
        }else if("/sys/devices/system/cpu/possible".equals(pathname)){ // 可以使用的cpu最多的数量，即能够上线的最大数量
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "0-7".getBytes(StandardCharsets.UTF_8)));
        }else if("/proc/self/auxv".equals(pathname)){
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath + "/wb/file/auxv"), pathname));
        }else if("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq".equals(pathname)){ // 查看cpu最大频率
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "1766400".getBytes()));
        }else if("/proc/meminfo".equals(pathname)){
//            emulator.getUnwinder().unwind();
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath + "/wb/file/meminfo"), pathname));
        }else if("/data".equals(pathname)){
//            emulator.attach().debug();
        }else if("/data/local/su".equals(pathname)){ // 访问权限文件 在 sub_56c8中实现 access
//            emulator.attach().debug();
        }else if(pathname.equals("/proc/"+emulator.getPid()+"/cmdline")){ // 紧跟 主函数is_root下 sub_4664 检测cmdline
            //  cmdline的格式默认以\0分割，如果不加\0，当样本以“逐个字节往后读，直到遇到\0”的逻辑解析时，会导致错误
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "com.sina.weibo\0".getBytes(StandardCharsets.UTF_8)));
        }else if("/data/app/com.sina.weibo-x6mgylYN7-50gCnw1ceNwA==/base.apk".equals(pathname)){ // 对假maps里补的apk路径进行 重定向为真apks
            // cmdline检测后，主函数检测app_sig，ida没办法直接定位，可通过对maps的调用堆栈
            // 查找目标函数为 sub_4700 中，本质上是对apk签名的校验————防止分析者直接把SO放到自己的项目里使用或分析调试
            // 一般的校验 是通过JNI方法获得，这里是通过maps则需要构造一个包含apk路径的maps，并对读取的apk路径重定向为真实apk路径
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath + "/wb/apks/wb.apk"), pathname));
        }else if("ps".equals(pathname)){ // 记录关于apk相关 ps信息 sys_epoll_wait
//            emulator.getUnwinder().unwind();
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath + "/wb/file/ps.txt"), pathname));
        }else if(pathname.equals("/proc/"+emulator.getPid()+"/status")){ // getpid 主函数中 sub_58C0 访问 "/proc/%d/status" 获得TracerPid
            return FileResult.success(new ByteArrayFileIO(oflags, pathname,String.valueOf(emulator.getPid()).getBytes(StandardCharsets.UTF_8)));
        }else if("/data/data/com.sina.weibo/../".equals(pathname)){ // 对目录检测 sub_6d64 发现存在access访问 粗略估计为root检测
//            emulator.getUnwinder().unwind();
        }else if("/sys/class/thermal/".equals(pathname)){ // 检查CPU温度，真机下有thermal_zone文件，模拟器不具备这系列文件 一般无权限
        }else if("/proc/sys/fs/binfmt_misc/".equals(pathname)){ // 检查模拟器 真机无 0x08efd
        }
        return null;
    }

    /**
     * 模拟调用
     */
    public void call(){
        String methodSing = "deal(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
        String arg1 = "{\"device_name\":\"PCLM10\"}";
        String arg2 = "01A7YCRFj4qPRAz9pseIeNExMiOIFdx5kXGLQTMyfXSSdpqnE.";
        String arg3 = "2AkMW";
        String arg4 = "5311_4002";
        String arg5 = "10B0095010";
        String arg6 = "11.0.0";
        String arg7 = "00000";
        cNative.newObject(null).callJniMethodObject(emulator, methodSing, arg1,arg2,arg3,arg4,arg5,arg6,arg7);
    }


}
