package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MTSign extends AbstractJni implements IOResolver {
    /**
     * Template: 美团 11.10.206.apk  -> main(111)/main(203)
     * Content: 涉及 文件环境补充 函数初始化
     * Method: 做文件映射环境补充 && 同一函数不同调用作为环境初始化 + jni-trace追
     * Algorithm: 暂无
     */
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final String dirPath = "src/main/resources/demo_resources";

    public MTSign() {

        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.mt.sign").build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        emulator.getSyscallHandler().addIOResolver(this); // 文件映射

        vm = emulator.createDalvikVM(new File(dirPath + "/mt/apks/mt.apk"));
        DalvikModule dm = vm.loadLibrary(new File(dirPath + "/mt/libmtguard.so"), true);
        vm.setJni(this);
        vm.setVerbose(true);
        module = dm.getModule();

        dm.callJNI_OnLoad(emulator);
    }

    public void mtMain111(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(111);
        DvmObject<?> dvmObject = vm.resolveClass("java/lang/Object").newObject(null);
        ArrayObject arrayObject = new ArrayObject(dvmObject);
        // 需要添加两次 本地对象
//        vm.addLocalObject(dvmObject);
//        vm.addLocalObject(arrayObject);
        list.add(vm.addLocalObject(arrayObject));
        module.callFunction(emulator, 0x5a38d, list.toArray());
    }

    public String mtMain(String str1, String str2){
        // 生成 main 参数为 203
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        // 第一个参数为int  第二个参数为object数组
        list.add(203);
        StringObject stringObject = new StringObject(vm, "9b69f861-e054-4bc4-9daf-d36ae205ed3e");
        ByteArray byteArray = new ByteArray(vm, "GET /aggroup/homepage/display __r0ysue".getBytes(StandardCharsets.UTF_8));
        DvmInteger dvmInteger = DvmInteger.valueOf(vm, 2); // integet对象
//        vm.addLocalObject(stringObject);
//        vm.addLocalObject(byteArray);
//        vm.addLocalObject(dvmInteger);
        list.add(vm.addLocalObject(new ArrayObject(stringObject, byteArray, dvmInteger))); // 完整参数
        Number number = module.callFunction(emulator, 0x5a38d, list.toArray());
        StringObject res = (StringObject) ((DvmObject[])((ArrayObject)vm.getObject(number.intValue())).getValue())[0];
        return res.getValue();
    }

    public String getSign(String str1, String str2){
        // 调用方法
        // 报错推出了 考虑：1。上下文缺失 2。检测unidbg
        // 1。so加载后到main函数调用前这段时间里，样本需要调用一些函数对so进行一些函数初始化（在jniload后直接调用方法 跳过可能存在的初始化阶段）
        // 找对应可能是初始化的函数 可能为native方法 根据动态打印的signature
        // 在java层进行hook 观察 那些在so加载进来就开始执行，并在每次之后都执行目标main（main函数的嫌疑最大。因为我们的参数1是203，
        // 可是参数1的潜在选择可不止这一种，或许其中某一个数作为参数1时，就充当着”激活函数“或者叫”初始化函数“）
        // 验证后：so加载后首先执行main函数 -》 main函数是初始化函数（参数为111） -》 main函数执行生成sign（参数为203）
        // 上述补java层环境 && 文件访问
        // (ARM32SyscallHandler:1896) - openat dirfd=-100
        // =》补文件访问 修改 unidbg-api/src/main/java/com/github/unidbg/file/BaseFileSystem.java BaseFileSystem root确定虚拟映射的文件夹在本地位置
        // 随后把对应apk 移为相同路径下
        mtMain111();
        return mtMain(str1, str2);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        // 通过jnitrace 打印 或者 hook 确定对应方法的值
        switch (signature){
            case "com/meituan/android/common/mtguard/NBridge->getPicName()Ljava/lang/String;":{
                return new StringObject(vm, "ms_com.sankuai.meituan");
            }
            case "com/meituan/android/common/mtguard/NBridge->getSecName()Ljava/lang/String;":{
                return new StringObject(vm, "ppd_com.sankuai.meituan.xbt");
            }
            case "com/meituan/android/common/mtguard/NBridge->getAppContext()Landroid/content/Context;":{
                return vm.resolveClass("android/content/Context").newObject(null);
            }
            case "com/meituan/android/common/mtguard/NBridge->getMtgVN()Ljava/lang/String;":{
                return new StringObject(vm, "4.4.7.3");
            }
            case "com/meituan/android/common/mtguard/NBridge->getDfpId()Ljava/lang/String;":{
                // 不确定就补 空 先试
                return new StringObject(vm, "");
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            // 这有问题  瞎几把写也可以 成功
            case "android/content/pm/PackageManager->GET_SIGNATURES:I":{
                // 这个方法是获得包的签名
                return 1100090405;
            }
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "android/content/Context->getPackageCodePath()Ljava/lang/String;":{
                // 获得 package的路径
                System.out.println("路径在这里");
                return new StringObject(vm, "/data/app/com.sankuai.meituan-TEfTAIBttUmUzuVbwRK1DQ==/base.apk");
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature){
            case "android/content/pm/PackageInfo->versionCode:I":{
                return 1100090405;
            }
        }
        return super.getIntField(vm, dvmObject, signature);
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/Integer-><init>(I)V":{
                // TODO 补构造函数 integer的构造函数
                int input = vaList.getIntArg(0);
                return vm.resolveClass("java/lang/Integer").newObject(input);
            }
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public FileResult resolve(Emulator emulator, String s, int i) {
        if("/data/app/com.sankuai.meituan-TEfTAIBttUmUzuVbwRK1DQ==/base.apk".equals(s)){
            return FileResult.success(new SimpleFileIO(i, new File(dirPath + "/mt/apks/mt.apk"), s));
        }
        return null;
    }
}
