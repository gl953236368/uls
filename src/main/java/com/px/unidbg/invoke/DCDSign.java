package com.px.unidbg.invoke;

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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Base64;
import java.util.List;

public class DCDSign extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final File dirPath = new File("");
    int mErrorCode;

    DCDSign(){
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.dcd.sign").build();
        final Memory memory = emulator.getMemory();

        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(dirPath.getAbsolutePath() + "/dcd/apks/dcd_6.5.1.apk"));
        DalvikModule dm = vm.loadLibrary(new File(dirPath.getAbsolutePath() + "/dcd/libcjtfcc.so"),true);

        vm.setVerbose(true);
        vm.setJni(this);
        module = dm.getModule();

        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public void setIntField(BaseVM vm, DvmObject<?> dvmObject, String signature, int value) {
        switch (signature){
            case "com/bdcaijing/tfccsdk/Tfcc->mErrorCode:I":{
                mErrorCode = value;
                System.out.println("mErrorCode:"+mErrorCode);
            }
        }
//        super.setIntField(vm, dvmObject, signature, value);
    }
    public void tfccDecrypt(){
        List<Object> list = new ArrayList<>(10);
        int number = 1;
        int flag = 13;
        String v1 = "14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8=";
        String v2 = "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";
        list.add(vm.getJNIEnv());
        list.add(vm.addLocalObject(vm.resolveClass("com/bdcaijing/tfccsdk/Tfcc").newObject(null)));
        list.add(number);
        list.add(flag);
        list.add(vm.addLocalObject(new StringObject(vm, v1)));
        list.add(vm.addLocalObject(new StringObject(vm, v2)));
        Number number1 = module.callFunction(emulator, 0xa2ac, list.toArray())[0];
        String reuslt = vm.getObject(number1.intValue()).getValue().toString();
        System.out.println("result:"+reuslt);
    }

    public void tfccEncrypt(){
        List<Object> list = new ArrayList<>(10);
        int number = 1; // 和解码一致
        String v1 = "14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8="; // 固定值
        String v3 = Base64.getEncoder().encodeToString(("dongchedi"+  System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        list.add(vm.getJNIEnv());
        list.add(vm.addLocalObject(vm.resolveClass("com/bdcaijing/tfccsdk/Tfcc").newObject(null)));

        list.add(number);
        list.add(1); // 固定值
        list.add(vm.addLocalObject(new StringObject(vm, v1))); //固定值
        list.add(vm.addLocalObject(new StringObject(vm, v3))); // dongchedi+毫秒时间戳

        Number value = module.callFunction(emulator, 0x8be4, list.toArray())[0];
        String t_key = vm.getObject(value.intValue()).getValue().toString();
        System.out.println("t_key:"+t_key);
    }

    public void doDebugger(){
        // 打印了日志 查看相关log函数 发现有三个 分别对三个进行断点，
        // 只有 0xa574被debugger到 观察if逻辑 函数存在 CMP R0 #0
        // deal: patch 函数 强行不走
        Debugger debugger = emulator.attach();

//        debugger.addBreakPoint(module.base + 0xa4B8 + 1);
//        debugger.addBreakPoint(module.base + 0xa574 + 1);
//        debugger.addBreakPoint(module.base + 0xa534 + 1);
//        debugger.addBreakPoint(module.base + 0xa504 + 1);


//        debugger.addBreakPoint(module.base + 0xae60 + 1);

        debugger.addBreakPoint(module.base + 0xae9c + 1);  // 正常解密路径
//        debugger.addBreakPoint(module.base + 0xae8c + 1);  // 非.......
    }
    public void patchVerify(){
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
            System.out.println("pathch_length:"+patch.length);
            System.out.println("code_length:"+code.length);
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
            System.out.println("pathch_length:"+patch.length);
            System.out.println("code_length:"+code.length);
            if(patch.length != code.length){
                throw new IllegalStateException(Inspector.inspectString(patch, "patch32 length=" + patch.length));
            }
            pointer.write(0, patch, 0, patch.length);

        }
    }

    public static void main(String[] args) {
        // debuge 模式 bt打堆栈  [0x40000000][0x400092b8][libcjtfcc.so][0x092b8]
        // =》 ida: BLX R2 查看r2寄存器的值 mr2
//        Logger.getLogger("com.github.unidbg.linux.ARM32SyscallHandler").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.unix.UnixSyscallHandler").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.AbstractEmulator").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.linux.android.dvm.DalvikVM").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.linux.android.dvm.BaseVM").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.linux.android.dvm").setLevel(Level.DEBUG);
        DCDSign dcdSign = new DCDSign();
        String v3 = Base64.getEncoder().encodeToString(("dongchedi"+  System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        byte[] bytes = Base64.getDecoder().decode(v3);
        String v4 = new String(bytes, StandardCharsets.UTF_8);
        System.out.println(v4);
        dcdSign.patchVerifyAdd4();
        dcdSign.patchVerify();

//        dcdSign.doDebugger();
        dcdSign.tfccDecrypt();
//        dcdSign.tfccEncrypt();
    }

}
