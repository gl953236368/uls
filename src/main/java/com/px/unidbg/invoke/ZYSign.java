package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.px.unidbg.config.UnidbgProperties;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ZYSign extends AbstractJni {
    /**
     * Template: 最右 573.apk  -> sign()
     * Content: 存在前置函数，补环境，记录unidbg主动调用native方法
     * Method: unidbg补充环境
     * Algorithm: md5加密 只修改了魔值
     */
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final String dirPath = "src/main/resources/demo_resources";
    private final Boolean isVerbose;

    public ZYSign(UnidbgProperties unidbgProperties) {
        isVerbose = unidbgProperties.isVerbose();
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.zy.sign").build();     // 创建模拟器
        final Memory memory = emulator.getMemory();  // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = emulator.createDalvikVM(new File(dirPath + "/zy/apks/zy573.apk")); // 依托原apk 创建android虚拟
        // 这里设置 不加载 so 会出现乱码的情况 shift+f7观察 init_array中 存在大量函数 推测在此存在 加解密过程
        DalvikModule dm = vm.loadLibrary(new File(dirPath + "/zy/libnet_crypto.so"), true); // 加载so到虚拟内存里
        module = dm.getModule(); // 获得本so模块的句柄

        vm.setJni(this);
        vm.setVerbose(isVerbose);
        dm.callJNI_OnLoad(emulator);
        log.info("最右module 加载成功");
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
        Number number = module.callFunction(emulator, 0x4a28d, list.toArray());
        String result = vm.getObject(number.intValue()).getValue().toString();
        return result;
    }

    public String getSign(String str1, String Str2){
        callNativeInit();
        String result = sign(str1, Str2);
        return result;
    }

    public static byte hexToByte(String inHex){
        return (byte) Integer.parseInt(inHex,16);
    }


    public static byte[] hexToBytes(String hex){
//        hex转byte数组
        if(hex.length() < 1){
            return null;
        }else {
            byte[] res = new byte[hex.length() / 2];
            int j = 0;
            for(int i = 0; i < hex.length(); i+=2){
                res[j++] = ZYSign.hexToByte(hex.substring(i, i+2));
            }
            return res;
        }
    }

    public String decodeAES(byte[] b1, Boolean flag){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        ByteArray byteArray = new ByteArray(vm, b1);
        list.add(vm.addLocalObject(byteArray));
        list.add(vm.addLocalObject(vm.resolveClass("java/lang/Boolean").newObject(flag)));
        Number number = module.callFunction(emulator, 0x4a14d, list.toArray());
        byte[] res = (byte[]) vm.getObject(number.intValue()).getValue();
//        System.out.println(new String(res));
        return new String(res);
    }

    public void getProtocolKey() {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        Number number = module.callFunction(emulator, 0x4a419, list.toArray());
        System.out.println("key:"+vm.getObject(number.intValue()).getValue().toString());
    }

    public byte[] encodeAES(){
        List<Object> list = new ArrayList<>(10);
//        String param1 = "{\"list\":[{\"action\":\"view\",\"otype\":\"partdetail\",\"src\":\"topicdetail\",\"id\":158,\"oid\":119357,\"data\":{\"st\":1638861103388,\"from_page\":\"search\",\"et\":1638861110461,\"source\":\"topicdetail\",\"remain_time_ms\":7072,\"remain_time\":7,\"c_ver\":\"5.7.3\",\"tid\":119357,\"part_id\":158,\"cur_page\":\"topicdetail\",\"log_id\":\"1638861110462_87\"}},{\"action\":\"background\",\"otype\":\"appuse\",\"src\":\"other\",\"data\":{\"st\":1638861100418,\"use_mod\":\"hot\",\"dur\":3,\"from_page\":\"search\",\"pushset\":0,\"et\":1638861103381,\"c_ver\":\"5.7.3\",\"cur_page\":\"topicdetail\",\"log_id\":\"1638861103389_86\"}}],\"h_av\":\"5.7.3\",\"h_dt\":0,\"h_os\":23,\"h_app\":\"zuiyou\",\"h_model\":\"Nexus 6P\",\"h_did\":\"eea5db7016d84e67\",\"h_nt\":1,\"h_m\":255505534,\"h_ch\":\"huawei\",\"h_ts\":1638861111361,\"token\":\"T0K5N6CAhK2BVDkwAV20SmNOfoBPqMGz4kYJiuVJgFNHErXxO07Jr2dpifwTru8X8cIiy1kNq8duoaEG_xJUU67ztVg==\",\"android_id\":\"eea5db7016d84e67\",\"h_ids\":{\"meid\":\"86797902030001\",\"imei2\":\"86797902030001\",\"imei1\":\"86797902030001\"}}";
        list.add(vm.getJNIEnv());
        list.add(0);
//        ByteArray byteArray = new ByteArray(vm, param1.getBytes(StandardCharsets.UTF_8));
        ByteArray byteArray1 = new ByteArray(vm, ZYSign.hexToBytes("7b226d6964223a32313533323639362c2274223a3130333930353634352c2266696c746572223a22616c6c222c22635f7479706573223a5b312c322c37302c32325d2c22685f6176223a22352e372e33222c22685f6474223a302c22685f6f73223a32332c22685f617070223a227a7569796f75222c22685f6d6f64656c223a224e65787573203650222c22685f646964223a2265656135646237303136643834653637222c22685f6e74223a312c22685f6d223a3235353530353533342c22685f6368223a22687561776569222c22685f7473223a313633393533393731323432342c22746f6b656e223a2254374b624e364341684b324256446b7741563230536d4e4f666f4f45764946684a3045685149437666377149565a4b49657146595f453367744a39593653522d6f787a513155395a556b587674386643477644564e56506f6a35413d3d222c22616e64726f69645f6964223a2265656135646237303136643834653637222c22685f696473223a7b226d656964223a223836373937393032303330303031222c22696d656932223a223836373937393032303330303031222c22696d656931223a223836373937393032303330303031227d7d"));
        list.add(vm.addLocalObject(byteArray1));
        Number number = module.callFunction(emulator, 0x4a0b9, list.toArray());
        byte[] res = (byte[]) vm.getObject(number.intValue()).getValue();
        return res;
    }

    public void replaceArgByConsoleDebugger() {
        emulator.attach().addBreakPoint(module.base + 0x5E1A2, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                String fakeInput = "8e 9d ad ad bc bc 4d 4d 4d 6c 6c 7b 7b 7b 8b 8b".replace(" ", "");
//                 0x403d2700

                // 由于不同接口补充的 地方不一致 所以 地址也不一致
                emulator.getBackend().mem_write(0x4043e000, hexToBytes(fakeInput));
                Inspector.inspect(emulator.getBackend().mem_read(0x4043e000, 16), " 0x40542000 修改明文");

//                emulator.getBackend().mem_write(0x40542000, hexToBytes(fakeInput));
//                Inspector.inspect(emulator.getBackend().mem_read(0x40542000, 16), " 0x40542000 修改明文");
                return true;
            }
        });

        emulator.attach().addBreakPoint(module.base + 0x5E140, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                String fakeInput = "c8 10 b6 c5 c5 d5 d5 65 65 75 94 94 a3 a3 b3 b3".replace(" ", "");
                emulator.getBackend().mem_write(0x403df040, hexToBytes(fakeInput));
                Inspector.inspect(emulator.getBackend().mem_read(0x403df040, 16), " 0x403df040 iv ");

//                String fakeInputKey = "cf 9d 9d ad ad bc bc cc cc eb 7b 7b 8b 8b 9a 9a".replace(" ", "");
                String fakeInputKey = "df bd bd cc cc cc dc dc dc 7c 7c 8b 8b 8b 9b 9b".replace(" ", "");

                emulator.getBackend().mem_write(0x403df050, hexToBytes(fakeInputKey));
                Inspector.inspect(emulator.getBackend().mem_read(0x403df050, 16), " 0x403df050 key ");

                return true;
            }
        });
    }
    public String getDecode(String hexStr){
        callNativeInit();
        replaceArgByConsoleDebugger();
        byte[] b1 = encodeAES();
        String result = decodeAES(ZYSign.hexToBytes(hexStr), true);
        return result;
    }

    public void destory() throws IOException {
        emulator.close();
        if (isVerbose) {
            log.info("destroy");
        }
    }

//    public static void main(String[] args) {
//        ZYSign zy = new ZYSign();
//        zy.callNativeInit(); // 初始化 函数
////        zy.hook65540(); // hook nativve 方法 被动
////        zy.callMd5();
//        System.out.println(zy.sign()); // 获得sign值
//    }
}
