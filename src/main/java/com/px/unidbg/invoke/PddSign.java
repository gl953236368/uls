package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

public class PddSign extends AbstractJni {
    /**
     * Template: 拼多多 5.72.apk  -> getInfo2()
     * Content: 涉及 lib依赖 以及 调用堆栈的补充（方法未调用前就需要补充很多环境 且 unidbg异常）
     * Method: unidbg补充非内置lib包(自写lib包引入) 以及 结合jnitrace 对指定参数进行补充 调用堆栈的环境补法
     * Algorithm: 暂无
     */
    //    com.xunmeng.pinduoduo
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final String dirPath = "src/main/resources/demo_resources";

    PddSign(){
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.pdd.sign").build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(dirPath + "/pdd/apks/pdd5.72.apk"));
        vm.setVerbose(false);
        vm.setJni(this);

        // 解决依赖库
        DalvikModule dm_dependency = vm.loadLibrary(new File(dirPath+ "/pdd/libUserEnv.so"), true);
        dm_dependency.callJNI_OnLoad(emulator);

        DalvikModule dm_dependency1 = vm.loadLibrary(new File(dirPath+ "/pdd/libc++_shared.so"), true);
        dm_dependency1.callJNI_OnLoad(emulator);

        DalvikModule dm = vm.loadLibrary(new File(dirPath+ "/pdd/libpdd_secure.so"),true);
        module = dm.getModule();


        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            case "android/provider/Settings$Secure->ANDROID_ID:Ljava/lang/String;":{
                return new StringObject(vm, "android_id");
            }
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "android/app/ActivityThread->getApplication()Landroid/app/Application;":{
                // 参考继承类 Application在Unidbg中并没有封装 所以要用signature
                return vm.resolveClass("android/app/Application", vm.resolveClass("android/content/ContextWrapper", vm.resolveClass("android/content/Context"))).newObject(signature);
            }
            case "android/content/Context->getContentResolver()Landroid/content/ContentResolver;":{
                // 参考继承类
                return vm.resolveClass("android/content/ContentResolver;").newObject(signature);
            }
            case "java/util/UUID->toString()Ljava/lang/String;":{
                UUID uuid = (UUID) dvmObject.getValue();
                return new StringObject(vm, uuid.toString());
            }
            case "java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":{
                String str = (String) dvmObject.getValue();
                String v1 = (String) vaList.getObjectArg(0).getValue();
                String v2 = (String) vaList.getObjectArg(1).getValue();
                return new StringObject(vm, str.replace(v1, v2));
            }

        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;":{
                // 获取 android_id为key的值 查询 android——id的值可以为空
                String arg = (String) vaList.getObjectArg(1).getValue();
                return new StringObject(vm, "");
            }
            case "java/util/UUID->randomUUID()Ljava/util/UUID;":{
                return vm.resolveClass("java/util/UUID").newObject(UUID.randomUUID());
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V":{
                return;
            }
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "android/content/Context->checkSelfPermission(Ljava/lang/String;)I":{
                // 通过jnitrace对照 补上值
                return -1;
            }
            case "android/telephony/TelephonyManager->getSimState()I": {
                // 通过jnitrace对照 补上值 自己的是5
                return 1;
            }
            case "android/telephony/TelephonyManager->getNetworkType()I": {
                // 通过jnitrace对照 补上值 自己的是0
                return 13;
            }
            case "android/telephony/TelephonyManager->getDataState()I":{
                // 通过jnitrace对照 补上值 自己的是0
                return 0;
            }
            case "android/telephony/TelephonyManager->getDataActivity()I": {
                // 通过jnitrace对照 补上值 自己的是0
                return 4;
            }
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;":{
                // 入参为 phone 返回为android.telephony.TelephonyManager对象
                String arg = (String) varArg.getObjectArg(0).getValue();
                // newObject中填入signature是为了做对象之间的区分，和代码逻辑无关
                return vm.resolveClass("android/telephony/TelephonyManager").newObject(signature);
            }
            case "android/telephony/TelephonyManager->getSimOperatorName()Ljava/lang/String;":
            case "android/telephony/TelephonyManager->getNetworkOperatorName()Ljava/lang/String;": {
                // 检测sim 卡 通过jnitrace 去观察 ReleaseStringUTFChars
                return new StringObject(vm, "中国联通");
            }
            case "android/telephony/TelephonyManager->getSimCountryIso()Ljava/lang/String;":
            case "android/telephony/TelephonyManager->getNetworkCountryIso()Ljava/lang/String;": {
                // 检测 通过jnitrace 去观察 ReleaseStringUTFChars
                return new StringObject(vm, "cn");
            }
            case "android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;": {
                // 检测 通过jnitrace 去观察 ReleaseStringUTFChars 本机为空
                return new StringObject(vm, "46001");
            }
            case "android/content/Context->getContentResolver()Landroid/content/ContentResolver;":{
                // 参照父类
                return vm.resolveClass("android/content/ContentResolver;").newObject(signature);
            }
            case "java/lang/Throwable->getStackTrace()[Ljava/lang/StackTraceElement;":{
                // 检测堆栈 防止 xposed 如果被Xposed Hook了，堆栈里会有Xposed这一层
                // 获得 数据长度、遍历调用完整的调用站
                // jnitrace 观察有releaseStringUTFChars 的 调用class
                StackTraceElement[] stackTraceElements = {
                        new StackTraceElement("com.xunmeng.pinduoduo.secure.DeviceNative", "", "", 0),
                        new StackTraceElement("com.xunmeng.pinduoduo.secure.SecureNative", "", "",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.secure.s", "", "",0),
                        new StackTraceElement("com.aimi.android.common.http.a","","",0),
                        new StackTraceElement("com.aimi.android.common.http.j","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.k","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.PQuicInterceptor","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.config.i$c","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.basekit.http.manager.b$4","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.o","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.e","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.b","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.a","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.m","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.c","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.j","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.internal.b.g","","",0),
                        new StackTraceElement("okhttp3.RealCall","","",0),
                        new StackTraceElement("com.aimi.android.common.http.unity.UnityCallFactory$a","","", 0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b.a","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.a.a","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b.b","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.a.a","","",0),
                        new StackTraceElement("1","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.g","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.g$a","","",0),
                        new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b","","",0),
                        new StackTraceElement("java.util.concurrent.ThreadPoolExecutor","","",0),
                        new StackTraceElement("java.util.concurrent.ThreadPoolExecutor$Worker","","",0),
                        new StackTraceElement("java.lang.Thread","","",0),
                };
                DvmObject[] objects = new DvmObject[stackTraceElements.length];
                for(int i=0;i<objects.length;i++){
                    objects[i] = vm.resolveClass("java/lang/StackTraceElement").newObject(stackTraceElements[i]);
                }
                return new ArrayObject(objects);
            }
            case "java/lang/StackTraceElement->getClassName()Ljava/lang/String;":{
                StackTraceElement element = (StackTraceElement) dvmObject.getValue();
                return new StringObject(vm, element.getClassName());
            }
            case "java/io/ByteArrayOutputStream->toByteArray()[B":{
                ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) dvmObject.getValue();
                byte[] result = byteArrayOutputStream.toByteArray();
                return new ByteArray(vm, result);
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;":{
                // 观察 getString 调用的 key 来补值
                String arg = (String) varArg.getObjectArg(1).getValue();
                if("android_id".equals(arg)){
                    return new StringObject(vm, "");
                }
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "android/os/Debug->isDebuggerConnected()Z":{
                return false;
            }
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "java/lang/Throwable-><init>()V":{
                // 补异常环境
//                return vm.resolveClass("java/lang/Throwable").newObject(new Throwable());
                return vm.resolveClass("java/lang/Throwable").newObject(signature);
            }
            case "java/io/ByteArrayOutputStream-><init>()V":{
                return vm.resolveClass("java/io/ByteArrayOutputStream").newObject(new ByteArrayOutputStream());
//                return vm.resolveClass("java/io/ByteArrayOutputStream").newObject(signature);
            }
            case "java/util/zip/GZIPOutputStream-><init>(Ljava/io/OutputStream;)V":{
                try {
                    OutputStream outputStream = (OutputStream) varArg.getObjectArg(0).getValue();
                    return vm.resolveClass("java/util/zip/GZIPOutputStream").newObject(new GZIPOutputStream(outputStream));
                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/util/zip/GZIPOutputStream->write([B)V":{
                // TODO byte Byte 区别
                GZIPOutputStream gzipOutputStream = (GZIPOutputStream) dvmObject.getValue();
                byte[] bytes = (byte[]) varArg.getObjectArg(0).getValue();
                try {
                    gzipOutputStream.write(bytes);
                    return;
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            case "java/util/zip/GZIPOutputStream->finish()V":{
                GZIPOutputStream gzipOutputStream = (GZIPOutputStream) dvmObject.getValue();
                try {
                    gzipOutputStream.finish();
                    return;
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            case "java/util/zip/GZIPOutputStream->close()V":{
                GZIPOutputStream gzipOutputStream = (GZIPOutputStream) dvmObject.getValue();
                try {
                    gzipOutputStream.close();
                    return;
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }

    public String getInfo2(){
        List<Object> list = new ArrayList<>();
        list.add(vm.getJNIEnv());
        list.add(0);
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        list.add(vm.addLocalObject(context));
        list.add(0x17AD420321AL); // 高位高地址
        Number  number = module.callFunction(emulator, 0xe3d5, list.toArray())[0];
        String result = vm.getObject(number.intValue()).getValue().toString();
//        System.out.println("result:"+result);
        return result;
    }

//    public static void main(String[] args) {
//        // 错误:1.依赖库 libUserEnv.so 发现为自写库 => 提前加载依赖库
//        // 2.jniversion 可先忽略
//        // 3. java 环境 补 frida hook + jnitrace 等
////        Logger.getLogger("com.github.unidbg.linux.ARM32SyscallHandler").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.unix.UnixSyscallHandler").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.AbstractEmulator").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm.DalvikVM").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm.BaseVM").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm").setLevel(Level.DEBUG);
//        PddSign sign = new PddSign();
//        System.out.println(sign.getInfo2());
////        File file = new File("");
////        System.out.println(file.getAbsolutePath());
//    }
}
