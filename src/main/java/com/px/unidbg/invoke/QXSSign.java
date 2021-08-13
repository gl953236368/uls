package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QXSSign extends AbstractJni {
    /**
     * Template: 轻小说   -> getSFSecurity()
     * Content: 涉及 文件环境补充 函数初始化
     * Method: 做文件映射环境补充 && 同一函数不同调用作为环境初始化 + jni-trace追 && unidbg自带的traceCode追踪
     * Algorithm: 确定为原生md5 但是不确定明文逻辑 nonce+timestamp+devicetoken+(固定的salt)td9#Kn_p7vUw
     */
    private final AndroidEmulator emulator;
    private final Module module;
    private final VM vm;
    private final String dirPath = "src/main/resources/demo_resources";

    public QXSSign() {
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.qxs.sign").build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(dirPath + "/qxs/apks/轻小说.apk"));

        DalvikModule dm = vm.loadLibrary(new File(dirPath+ "/qxs/libsfdata.so"), false);
        module = dm.getModule(); // 获得so操作句柄

        // unidbg 自带的 traceCode功能 追踪汇编指令流 (修改了输出打印每条汇编指令里参与运算的寄存器的值) 执行的时候 会存在bug
//        saveTraceCOde(dirPath);

        vm.setJni(this);
        vm.setVerbose(false);
        dm.callJNI_OnLoad(emulator);
    }

    public void saveTraceCOde(String dirPath){
        // 保存tracecode到文件
        String traceFile = dirPath+ "/qxs/qxstracecode1.txt";
        try {
            PrintStream traceStream = new PrintStream(new FileOutputStream(traceFile), true);
            emulator.traceCode(module.base, module.base+module.size).setRedirect(traceStream);
            System.out.println("trace COde 写入》》》》》》》》》"+traceFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getSFSecurity(String content){
        // eg. content: F1517503-9779-32B7-9C78-F5EF501102BC
        // sign :6A92FEE138CE83DE23E479A07C2CB12F
        // A->f4f0f8d7->6a92fee1 前八位 /B->
        // 分析trace
        // 1.搜索md5的几个魔值 和 md5的ktable值 发现出现
        // =》 可能为md5加密或者md5魔改 =》从trace中分析md5结果->确认输出和md5有关系/ 从trace中分析md5输入->确认函数输入和md5输入关系
        // 1.1 从trace中分析md5结果->确认输出和md5有关系
        // 找最后出现的魔值A=0x67452301/0xefcdab89/0x98badcfe/0x10325476
        // 计算相加合 （小于 0xffffffff 取低32bit）（大于 512bit 调整端序 逆向输出）  =》这就是md5的前八位数
        // 如果还能搜到 说明明文长度超过分组数 需要第二个分组计算 以此类推 =》确定为md5加密
        // 2.Trace汇编中析出MD5的明文
        // * 在md5流程中，每轮运算需要64步，每步操作的第三个操作是选取明文的一截进行加法运算，第四个操作是和K相加。
        // 不好定位第三个操作，第四个操作的K是已知的，可以描述为："第四个操作上方第一个add运算就是明文中的一截+中间结果"
        // （在生成过程中 四个操作 没有硬性顺序要求=》汇编代码 不遵守顺序生成）
        // 第一个 F(B,C,D) 的结果固定是 0xffffffff，基于K值 和 这个锚点，可以在trace里分析明文
        // => 第一个k相加位置 向上找 0xffffffff 的add汇编 对应的就是部分明文(16进制 右边为高位【逆序】)
        // ......
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        DvmObject<?> dvmObject = vm.resolveClass("android/content/Context").newObject(null);
        list.add(vm.addLocalObject(dvmObject));
        list.add(vm.addLocalObject(new StringObject(vm , content)));
        // 关于方法的位置 jni初始化 无地址 -》在function id搜不到 -》 exports里观察【一般是存在保护机制 没办法直接f5反编译】
        Number number = module.callFunction(emulator, 0xA944 + 1, list.toArray())[0]; // +1 thumb模式 arm不用加
        String result = vm.getObject(number.intValue()).getValue().toString();
        return result;
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "java/util/UUID->randomUUID()Ljava/util/UUID;":{
                return dvmClass.newObject(UUID.randomUUID());
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/util/UUID->toString()Ljava/lang/String;":{
                String uuid = dvmObject.getValue().toString();
                return new StringObject(vm, uuid);
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }
}
