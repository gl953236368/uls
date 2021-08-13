package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class ZYSign extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final String dirPath = "src/main/resources/demo_resources";


    public ZYSign() {
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.zy.sign").build();     // 创建模拟器
        final Memory memory = emulator.getMemory();  // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = emulator.createDalvikVM(new File(dirPath + "/zy/apks/zy573.apk")); // 依托原apk 创建android虚拟
        // 这里设置 不加载 so 会出现乱码的情况 shift+f7观察 init_array中 存在大量函数 推测在此存在 加解密过程
        DalvikModule dm = vm.loadLibrary(new File(dirPath + "/zy/libnet_crypto.so"), true); // 加载so到虚拟内存里
        module = dm.getModule(); // 获得本so模块的句柄

        vm.setJni(this);
        vm.setVerbose(false);
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/izuiyou/common/base/BaseApplication->getAppContext()Landroid/content/Context;":{
                // 先填空测试
                return vm.resolveClass("android/content/Context").newObject(null);
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "android/content/Context->getClass()Ljava/lang/Class;":{
                // dvmObject.getObjectType =》返回 方法对象类型
//                System.out.println(dvmObject.getObjectType());
                return dvmObject.getObjectType();
            }
            case "java/lang/Class->getSimpleName()Ljava/lang/String;":{
                // getSimpleName得到类的名称 可打堆栈 确定 这个方法参数
                // 主要流程 BaseApplication->获取到context->拿到对应上下文的class->获取到class的名称
                return new StringObject(vm, "AppController");
            }
            case "android/content/Context->getFilesDir()Ljava/io/File;":
            case "java/lang/String->getAbsolutePath()Ljava/lang/String;":{
                // 获取路径 校验SO是否在本App内执行。”补“+”修复“循环往复，下面一连补两个签名，返回值都根据实际APP情况
                return new StringObject(vm, "/data/user/0/cn.xiaochuankeji.tieba/files");
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Debug->isDebuggerConnected()Z":{
                return false;
            }
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Process->myPid()I":{
                // 可默认返回 模拟器的id
                return emulator.getPid();
            }
        }
        return super.callStaticIntMethodV(vm, dvmClass, signature, vaList);
    }

    // 测试hook
    public void hook65540(){
        IHookZz hookZz = HookZz.getInstance(emulator);
        // 不要管 md5=xx, hex=xx都是固定格式
        hookZz.wrap(module.base + 0x65540 + 1, new WrapCallback<HookZzArm32RegisterContext>() {
            @Override
            // frida onEnter
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                // 类似于frida args[0]
                // 发现参数1 就是传递的值 参数3可能是存在的buffer 参数2 长度和 传进来的值一致 应该就是长度
                Inspector.inspect(ctx.getR0Pointer().getByteArray(0, 0x10), "Arg1");
                System.out.println(ctx.getR1Long());
                Inspector.inspect(ctx.getR2Pointer().getByteArray(0, 0x10), "Arg3");
                // push 执行前push保存，在后面再pop取出
                ctx.push(ctx.getR2Pointer());
            };
            @Override
            // frida onLeave
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                // pop 取出
                // 经过对 输出 的 16位 数据  F7 8D CC 15 9A 7F 9F C4 54 0D 5C 53 16 BA 36 65定位 恰好是魔改后的md5的值
                // 经过对iv进行更改确定
                Pointer out = ctx.pop();
                Inspector.inspect(out.getByteArray(0, 0x10), "Args after execute");
            }
        });
    }

    public void callMd5(){
        // unidbg 主动来 hook 对应的native 方法
        List<Object> list = new ArrayList<>(10);

        // arg1
        String input = "gaolei";
        // 开辟一个运行空间
        MemoryBlock memoryBlock = emulator.getMemory().malloc(16, false);
        UnidbgPointer input_ptr = memoryBlock.getPointer();
        input_ptr.write(input.getBytes(StandardCharsets.UTF_8)); // arg1 被写入这个空间里二进制

        // arg2
        int inputLength = input.length();

        // arg3
        MemoryBlock memoryBlock1 = emulator.getMemory().malloc(16, false);
        UnidbgPointer buffer_ptr = memoryBlock1.getPointer();

        // 入参
        list.add(input_ptr);
        list.add(inputLength);
        list.add(buffer_ptr);

        // 调用
        module.callFunction(emulator, 0x65540+1, list.toArray());
        // 输出最后buffer里的值
        Inspector.inspect(buffer_ptr.getByteArray(0, 0x10), "md5 execute");
    }

    public void callNativeInit(){
        // 初始化方法
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        module.callFunction(emulator, 0x4a069, list.toArray());
    }

    public String sign(String str1, String Str2){
        // sign生成
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        //
        list.add(vm.addLocalObject(new StringObject(vm, str1)));
        ByteArray byteArray = new ByteArray(vm, Str2.getBytes(StandardCharsets.UTF_8));
        list.add(vm.addLocalObject(byteArray));
        Number number = module.callFunction(emulator, 0x4a28d, list.toArray())[0];
        String result = vm.getObject(number.intValue()).getValue().toString();
        return result;
    }

    public String getSign(String str1, String Str2){
        callNativeInit();
        String result = sign(str1, Str2);
        return result;
    }
//    public static void main(String[] args) {
//        ZYSign zy = new ZYSign();
//        zy.callNativeInit(); // 初始化 函数
////        zy.hook65540(); // hook nativve 方法 被动
////        zy.callMd5();
//        System.out.println(zy.sign()); // 获得sign值
//    }
}
