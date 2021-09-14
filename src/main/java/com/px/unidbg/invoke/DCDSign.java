package com.px.unidbg.invoke;

import com.alibaba.fastjson.JSON;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;



import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;

import okhttp3.*;
import org.springframework.stereotype.Component;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Base64;
import java.util.List;


@Component
public class DCDSign extends AbstractJni {
    /***
     * Template: 懂车帝 6.5.1.apk -》tfccEncrypt/tfccDecrypt（解决传值加密）
     * Content: 环境补充、patch目标指令（也可自己改so文件, 我试了直接改貌似初始化的时候有问题、解密的值为空）【其实可以不用patch】、
     *          存在时间校验（加密解密需要先后进行，不然会解密失败）
     * Method：js定位方法、consolerdebugger打端点 结合 ida汇编观察
     * Algorithm：暂无
     * Question: 貌似是运行期间没办法单独调用加密和解密算法，这两个方法需要成对调用（这个时候就不用patch）,
     *          也就是外部调用的时候需要请求完加密的t_key，就要去请求解密，保证在运行期间加密和解密成对出现
     *          不然的话会出现解密异常（场景：在一次请求加密完，不去解密，再去请求加密会有问题，强调成对）;
     */
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final String dirPath = "src/main/resources/demo_resources";
    int mErrorCode;

    DCDSign(){
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.dcd.sign").build();
        final Memory memory = emulator.getMemory();

        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(dirPath + "/dcd/apks/dcd_6.5.1.apk"));
        DalvikModule dm = vm.loadLibrary(new File(dirPath + "/dcd/libcjtfcc.so"),true);

        vm.setVerbose(false);
        vm.setJni(this);
        module = dm.getModule();

        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public void setIntField(BaseVM vm, DvmObject<?> dvmObject, String signature, int value) {
        switch (signature){
            case "com/bdcaijing/tfccsdk/Tfcc->mErrorCode:I":{
                mErrorCode = value;
//                System.out.println("mErrorCode:"+mErrorCode);
            }
        }
    }
    public String  tfccDecrypt(int number, int flag, String v1, String v2){
        // 解密参数 获取最终揭秘后的值 （base64）
        List<Object> list = new ArrayList<>(10);
        DvmClass Tfcc = vm.resolveClass("com/bdcaijing/tfccsdk/Tfcc");
        DvmObject<?> TfccObject = Tfcc.newObject(null);

        list.add(vm.getJNIEnv());
        list.add(vm.addLocalObject(TfccObject));
        list.add(number);
        list.add(flag);
        list.add(vm.addLocalObject(new StringObject(vm, v1)));
        list.add(vm.addLocalObject(new StringObject(vm, v2)));
        Number number1 = module.callFunction(emulator, 0xa2ac, list.toArray())[0];
        String reuslt = vm.getObject(number1.intValue()).getValue().toString();
        byte[] bytes = Base64.getDecoder().decode(reuslt);
        String v4 = new String(bytes, StandardCharsets.UTF_8);
        return v4;
    }

    public String tfccEncrypt(int number, int flag, String v1, String v3){
        // 加密 获得t_key
        List<Object> list = new ArrayList<>(10);
        DvmClass Tfcc = vm.resolveClass("com/bdcaijing/tfccsdk/Tfcc");
        DvmObject<?> TfccObject = Tfcc.newObject(null);

        list.add(vm.getJNIEnv());
        list.add(vm.addLocalObject(TfccObject));

        list.add(number);
        list.add(flag); // 固定值
        list.add(vm.addLocalObject(new StringObject(vm, v1))); //固定值
        list.add(vm.addLocalObject(new StringObject(vm, v3))); // dongchedi+毫秒时间戳

        Number value = module.callFunction(emulator, 0x8be4, list.toArray())[0];
        String t_key = vm.getObject(value.intValue()).getValue().toString();
//        System.out.println("t_key:"+t_key);
        return t_key;
    }

    public void doDebugger(){
        // 打印了日志 查看相关log函数 发现有三个 分别对三个进行断点，
        // 只有 0xa574被debugger到 观察if逻辑 函数存在 CMP R0 #0
        // deal: patch 函数 强行不走
        Debugger debugger = emulator.attach();

        // 控制三个log函数的位置 以及 存在问题的if的位置
//        debugger.addBreakPoint(module.base + 0xa4B8 + 1);  // 确定为此处if存在问题
//        debugger.addBreakPoint(module.base + 0xa574 + 1);  // 此处被debugger
//        debugger.addBreakPoint(module.base + 0xa534 + 1);
//        debugger.addBreakPoint(module.base + 0xa504 + 1);

//        debugger.addBreakPoint(module.base + 0xae60 + 1); // 控制真正揭秘路径的if
        debugger.addBreakPoint(module.base + 0xae9c + 1);  // 正常解密路径
//        debugger.addBreakPoint(module.base + 0xae8c + 1);  // 非.......
    }

    public void patchVerify(){
        // patch掉 if跳转错误 log打印问题
        // 这里不加1 arm (thumb 需要加一)
        Pointer pointer = UnidbgPointer.pointer(emulator, module.base + 0xa4B8);
        assert  pointer != null;
        byte[] code = pointer.getByteArray(2, 2);
        // 00 00 50 e3  cmp r0, #0
        if(!Arrays.equals(code, new byte[]{(byte) 0x50, (byte) 0xe3 })){
            throw new IllegalStateException(Inspector.inspectString(code, "patch32 code=" + Arrays.toString(code)));
        }
        try(Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb)){
            // 修改
            KeystoneEncoded keystoneEncoded = keystone.assemble("cmp r0, #1");
            byte[] patch = keystoneEncoded.getMachineCode();
//            System.out.println("pathch_length:"+patch.length);
//            System.out.println("code_length:"+code.length);
            if(patch.length != code.length){
                throw new IllegalStateException(Inspector.inspectString(patch, "patch32 length="+patch.length));
            }
            pointer.write(0, patch, 0, patch.length);
        }

    }

    public void patchVerifyAdd4(){
        Pointer pointer = UnidbgPointer.pointer(emulator, module.base + 0xae60);
        assert pointer != null;
        byte[] code = pointer.getByteArray(2, 2);
        // 00 00 50 e3 => cmp r0, #0
        if(!Arrays.equals(code, new byte[]{(byte) 0x50, (byte) 0xe3})){
            throw new IllegalStateException(Inspector.inspectString(code, "patch32 code="+Arrays.toString(code)));
        }
        try(Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb)){
            // 修改原始汇编
            KeystoneEncoded keystoneEncoded = keystone.assemble("cmp r0, #1");
            byte[] patch = keystoneEncoded.getMachineCode();
//            System.out.println("pathch_length:"+patch.length);
//            System.out.println("code_length:"+code.length);
            if(patch.length != code.length){
                throw new IllegalStateException(Inspector.inspectString(patch, "patch32 length=" + patch.length));
            }
            pointer.write(0, patch, 0, patch.length);
        }
    }

    public void testRequest() throws IOException {
        // 测试：本地生成 + 请求数据
        String v3 = "14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8=";
        String v4 = Base64.getEncoder().encodeToString(("dongchedi"+  System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        String t_key = tfccEncrypt(85, 1, v3, v4);
        RequestBody formBody = new FormBody.Builder().add("t_key", t_key).build();
        String url = "https://ib-hl.snssdk.com/motor/owner_price_api/car_price_list?car_id=48029&owner_price_region_name=%E5%8C%97%E4%BA%AC&sort_type=time_reverse&offset=0&page_size=10&iid=1293452139037208&device_id=60342758270&ac=wifi&channel=youdao_dcd_and8&aid=36&app_name=automobile&version_code=651&version_name=6.5.1&device_platform=android&ab_client=a1%2Cc2%2Ce1%2Cf2%2Cg2%2Cf7&ab_group=2589722&ssmix=a&device_type=Nexus+6P&device_brand=google&language=zh&os_api=23&os_version=6.0.1&manifest_version_code=651&resolution=1440*2392&dpi=560&update_version_code=6512&_rticket=1631585724661&cdid=03d9ca23-8093-4aa3-91f2-c7540ce3d8b9&city_name=%E5%8C%97%E4%BA%AC&gps_city_name=%E5%8C%97%E4%BA%AC&selected_city_name&rom_version=23&longi_lati_type=1&longi_lati_time=1631585696433&content_sort_mode=0&total_memory=2.73&cpu_name=Qualcomm+Technologies%2C+Inc+MSM8994&overall_score=4.8955&cpu_score=4.9524&host_abi=armeabi-v7a";
        String headers = "Dalvik/2.1.0 (Linux; U; Android 6.0.1; Nexus 6P Build/MTC20L) automobile/6.5.1 cronet/TTNetVersion:921ec9e4 2021-07-19 QuicVersion:6ad2ee95 2021-04-06";
        OkHttpClient httpClient = new OkHttpClient();
        String data = "";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("user-agent", headers)
                .post(formBody)
                .build();
        Response  response = httpClient.newCall(request).execute();
        String body = response.body().string();
        data = JSON.parseObject(body).getString("data");
        if(!"".equals(data)) {
            String result = tfccDecrypt(85,1, v3, data);
            System.out.println(result);
        }
    }

    public String runEncrypt(){
        // 调用加密接口
        int number = 10;
        int flag = 1;
        String v1 = "14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8=";
        String v2 = Base64.getEncoder().encodeToString(("dongchedi"+  System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        String t_key = tfccEncrypt(number, flag, v1, v2);
        return t_key;
    }

    public String runDecrypt(String encrypted){
        // 调用揭秘接口
        int number = 10;
        int flag = 1;
        String v1 = "14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8=";
        String result = tfccDecrypt(number, flag, v1, encrypted);
        return result;
    }

//        public static void main(String[] args) throws IOException {
//        // debuge 模式 bt打堆栈  [0x40000000][0x400092b8][libcjtfcc.so][0x092b8]
//        // =》 ida: BLX R2 查看r2寄存器的值 mr2
////        Logger.getLogger("com.github.unidbg.linux.ARM32SyscallHandler").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.unix.UnixSyscallHandler").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.AbstractEmulator").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm.DalvikVM").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm.BaseVM").setLevel(Level.DEBUG);
////        Logger.getLogger("com.github.unidbg.linux.android.dvm").setLevel(Level.DEBUG);
//        DCDSign dcdSign = new DCDSign();
////        dcdSign.patchVerifyAdd4();
////        dcdSign.patchVerify();
//
////        dcdSign.doDebugger();
////        dcdSign.tfccDecrypt();
////        String t_key = dcdSign.tfccEncrypt();
////        System.out.println(t_key);
//        dcdSign.testRequest();
//    }
}