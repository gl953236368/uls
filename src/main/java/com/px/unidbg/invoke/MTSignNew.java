package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.SystemPropertyHook;
import com.github.unidbg.linux.android.SystemPropertyProvider;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.ApplicationInfo;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.linux.android.dvm.wrapper.DvmLong;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MTSignNew extends AbstractJni implements IOResolver {
    /**
     * v11.12.403
     */
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final File dirPath = new File("");

    public MTSignNew() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .setRootDir(new File("target/rootfs"))
                .build();

        emulator.getSyscallHandler().addIOResolver(this); // 绑定文件重定向
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        // hook 出来相关调用
        // ********************************
        SystemPropertyHook systemPropertyHook = new SystemPropertyHook(emulator); // 获取属性的方法 掌握样本对于某个系统属性的请求 adb shell getprop xx 获得属性查看
        systemPropertyHook.setPropertyProvider(new SystemPropertyProvider() {
            @Override
            public String getProperty(String key) {
                System.out.println("fuck system_key:" + key);
                switch (key){
                    case "ro.kernel.qemu":
                    case "libc.debug.malloc":{
                        return "";
                    }
                    case "ro.build.user":{
                        return "android-build";
                    }
                    case "ro.build.host":{
                        return "wpiv1.hot.corp.google.com";
                    }
                    case "ro.build.display.id":{
                        return "OPM7.181205.001";
                    }
                }
                return "";
            }
        });
        memory.addHookListener(systemPropertyHook);


        vm = emulator.createDalvikVM(new File(dirPath.getAbsolutePath() + "/mt/apks/mt.apk"));
//        new AndroidElfLoader()

        // hook libc 里的popen 方法
        DalvikModule dmLibc = vm.loadLibrary(new File("unidbg-android/src/main/resources/android/sdk23/lib/libc.so"), true);
        Module dmLibcModule = dmLibc.getModule();
        int popenAddress =(int) dmLibcModule.findSymbolByName("popen").getAddress();

        emulator.attach().addBreakPoint(popenAddress, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext registerContext = emulator.getContext();
                String command = registerContext.getPointerArg(0).getString(0);
                System.out.println("liac popen command: "+command);
                emulator.set("command", command);
                return true;
            }
        });

        new AndroidModule(emulator, vm).register(memory); // 虚拟依赖注册
        DalvikModule dm = vm.loadLibrary(new File(dirPath.getAbsolutePath() + "/mt/libmtguard.so"), true);




        vm.setVerbose(true);
        module = dm.getModule();
        vm.setJni(this);

        dm.callJNI_OnLoad(emulator);
    }

//    public static void main(String[] args) {
//        MTSignNew mtSign = new MTSignNew();
//        mtSign.callInit();
//        mtSign.callTarget();
//    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("载入路径："+pathname);
        // 两种补文件的方式 第一个直接引入文件位置/第二个直接写入值
        if("/sys/class/power_supply/battery/temp".equals(pathname)){
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath.getAbsolutePath()+"/mt/temp"), pathname));
        }else if("/sys/class/power_supply/battery/voltage_now".equals(pathname)){
            return FileResult.<AndroidFileIO>success(new ByteArrayFileIO(oflags, pathname, "4243139".getBytes()));
        }

        if(pathname.equals("/data/app/com.sankuai.meituan-BdxZ6EllcZ9FJkEvINgj1w==/base.apk")){
            return FileResult.success(new SimpleFileIO(oflags, new File(dirPath.getAbsolutePath()+"/mt/apks/mt.apk"), pathname));
        }
        return null;
    }

    public void callInit(){
        List<Object> list = new ArrayList<>();
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(1);
        DvmObject<?> object = vm.resolveClass("java/lang/Object").newObject(null);
        ArrayObject arrayObject = new ArrayObject(object);
        list.add(vm.addLocalObject(arrayObject));
        Number number =module.callFunction(emulator, 0x41bd, list.toArray())[0];

        ArrayObject arrayObject1 = vm.getObject(number.intValue());
        DvmInteger dvmInteger = (DvmInteger) arrayObject1.getValue()[0];
        System.out.println("resutl:"+dvmInteger.getValue()); // 结果不一致 目标结果是 0 ？观察最后访问了俩文件
    }

    public void callTarget(){
        List<Object> list = new ArrayList<>();
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(2);
        StringObject stringObject = new StringObject(vm, "9b69f861-e054-4bc4-9daf-d36ae205ed3e");
        ByteArray byteArray = new ByteArray(vm, "GET /api/entry/indexLayer __reqTraceID=d0c4bfa5-9de0-44e3-87cd-42567c81b2b1&ci=1&msid=&topic_session_id=cfdefb48-b31f-454e-82d8-aeb11459e4ef&userid=-1&utm_campaign=AgroupBgroupC0E0Ghomepage&utm_content=86797902030455&utm_medium=android&utm_source=wandoujia&utm_term=1100120403&uuid=000000000000050BF607BA1A243C09D07DBE3C57DF8A6A164557486186355524&version_name=11.12.403".getBytes());
        DvmInteger dvmInteger = DvmInteger.valueOf(vm, 2);
        ArrayObject arrayObject = new ArrayObject(stringObject, byteArray, dvmInteger);
        list.add(vm.addLocalObject(arrayObject));
        Number number =module.callFunction(emulator, 0x41bd, list.toArray())[0];
        // 不确定怎么转换结果类型 可以打断点 express 测试 什么类型输出
        ArrayObject arrayObject1 = vm.getObject(number.intValue());
        StringObject stringObject1 = (StringObject) arrayObject1.getValue()[0];
        System.out.println(stringObject1);

    }
    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/meituan/android/common/mtguard/NBridge->getClassLoader()Ljava/lang/ClassLoader;":{
                // 先搜abstractjni
                // return new ClassLoader(vm, signature);: methodID问题 利用官方的异常（dalvik/system/PathClassLoader 手动继承测试
                // vm.resoleveClass 返回的是目标class 观察调用方法 确定是需要class 还是需要 object
                return vm.resolveClass("java/lang/ClassLoader").newObject(null); // 此处返回的是一个对象 需要加上 newObject
            }
            case "java/lang/Class->main2(I[Ljava/lang/Object;)Ljava/lang/Object;":{
                // 不确定可在此处打断点 确定参数信息 考虑往下补充
                System.out.println("debuge");
                // return vm.resolveClass("java/lang/Object").newObject(signature); error: .DvmObject cannot be cast to class .StringObject 转换类型失败 观察jni输出情况
                // jnitrace -l libmtguard.so com.sankuai.meituan >> 保存路径
                System.out.println("call main2"); // 通过调用方法 以及线程搜索结果 发现调用多次 com.sankuai.meituan
                int args1 = vaList.getIntArg(0); // 参数一一直在变
                System.out.println("args1: "+args1);
                switch (args1){
                    case 1:return new StringObject(vm, "com.sankuai.meituan");
                    case 4:return new StringObject(vm, "ms_com.sankuai.meituan");
                    case 5:return new StringObject(vm, "ppd_com.sankuai.meituan.xbt");
                    case 2:return vm.resolveClass("android/content/Context").newObject(null);  //先补空 jnitrace里是个对象 配合代码观察
                    case 6:return new StringObject(vm, "5.1.9");
                    case 3:
                    case 8:
                    case 40:return new StringObject(vm, "");
                }

            }
            case "java/lang/System->getProperty(Ljava/lang/String;)Ljava/lang/String;":{
                String arg1 = vaList.getObjectArg(0).getValue().toString();
                // 检测是否挂代理
                System.out.println("java/lang/System->getProperty:"+arg1);
                switch (arg1){ // 最好不要硬编码 不要固定写死值
                    case "http.proxyHost":
                    case "https.proxyHost": return new StringObject(vm, "");
                }
            }
            case "android/os/SystemProperties->get(Ljava/lang/String;)Ljava/lang/String;":{
                // 打断点确定值是什么
                String arg1 = vaList.getObjectArg(0).getValue().toString();
                System.out.println("android/os/SystemProperties:"+arg1);
                switch (arg1){
                    case "ro.build.id": return new StringObject(vm, "OPM7.181205.001");
                    case "persist.sys.usb.config":return new StringObject(vm, "adb");
                    case "sys.usb.config":return new StringObject(vm, "mtp,adb");
                    case "sys.usb.state": return new StringObject(vm, "mtp,adb"); // ？这三个状态表示当前手机的状态 是否在adb或其他调试模式 肯定否
                }

            }
            case "java/util/UUID->randomUUID()Ljava/util/UUID;":{
                // unidbg 借用java内的uuid实现结果
                return dvmClass.newObject(UUID.randomUUID());
            }
            case "java/lang/Long->valueOf(J)Ljava/lang/Long;":{
                // unidbg 内部封装了的基本类型可以直接掉 不用dvmclass引用新对象
                return DvmLong.valueOf(vm, vaList.getLongArg(0));
            }
            case "java/lang/String->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;":{
                // 借用参数 调用方法 创建对象 返回给系统
                String str1 = vaList.getObjectArg(0).getValue().toString();
                Long str2 = ((Long)((DvmLong)((DvmObject[])((ArrayObject)vaList.getObjectArg(1)).getValue())[0]).getValue()); // 参数类型有问题 通过断点进行 参数express 输出测试
                return new StringObject(vm, String.format(str1, str2));
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/ClassLoader->loadClass(Ljava/lang/String;)Ljava/lang/Class;":{
                return vm.resolveClass("java/lang/Class");
            }
//            case "java/lang/String->getPackageManager()Landroid/content/pm/PackageManager;":{
            // 前面补过了 context处理了
//                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
//            }
            case "android/content/pm/PackageManager->getApplicationInfo(Ljava/lang/String;I)Landroid/content/pm/ApplicationInfo;":{
                // TODO 抄的AbstractJni里的
                return new ApplicationInfo(vm);
            }
            case "java/util/UUID->toString()Ljava/lang/String;":{
                // 捕获到传参调用结果
                UUID uuid = (UUID) dvmObject.getValue();
                return new StringObject(vm, uuid.toString());
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "java/io/File-><init>(Ljava/lang/String;)V":{
                // 初始化文件了
                return dvmClass.newObject(vaList.getObjectArg(0).getValue().toString());
            }
            case "java/lang/Integer-><init>(I)V":{
                return DvmInteger.valueOf(vm, vaList.getIntArg(0));
            }
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/io/File->canRead()Z":{
                return true;
            }
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature){
            case "android/content/pm/ApplicationInfo->sourceDir:Ljava/lang/String;":{
                // jni 补app路径
                return new StringObject(vm, "/data/app/com.sankuai.meituan-BdxZ6EllcZ9FJkEvINgj1w==/base.apk");
            }
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            case "android/content/pm/PackageManager->GET_SIGNATURES:I":{
                // jnitrace 不出来玄学问题
                return 64;
            }
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature){
            case "android/content/pm/PackageInfo->versionCode:I":{
                return 1100120403;
            }
        }
        return super.getIntField(vm, dvmObject, signature);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            case "android/os/Build->BRAND:Ljava/lang/String;":{
                return new StringObject(vm, "google");
            }
            case "android/os/Build->TYPE:Ljava/lang/String;":{
                return new StringObject(vm, "user");
            }
            case "android/os/Build->HARDWARE:Ljava/lang/String;":{
                return new StringObject(vm, "angler");
            }
            case "android/os/Build->MODEL:Ljava/lang/String;":{
                return new StringObject(vm, "Nexus 6P");
            }
            case "android/os/Build->TAGS:Ljava/lang/String;":{
                return new StringObject(vm, "release-keys");
            }
            case "android/os/Build$VERSION->RELEASE:Ljava/lang/String;":{
                return new StringObject(vm, "8.1.0");
            }
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public long callStaticLongMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/System->currentTimeMillis()J":{
                // 直接调用系统的
                return System.currentTimeMillis();
            }
        }
        return super.callStaticLongMethodV(vm, dvmClass, signature, vaList);
    }
}
