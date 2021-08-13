package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.px.unidbg.utils.BiliTools;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


public class BiliSign extends AbstractJni {
    /**
     * Template: Bilibili 6.18.0.apk  -> s()
     * Content: 涉及 补充内部类 && 方法 使用继承类补对象
     * Method: 为了防止方法硬编码 引入对应调用方法 && 补充内部类 + jni-trace追
     * Algorithm: md5(明文字符串拼接 + 560c52cc + d288fed0 + 45859ed1 + 8bffd973)
     */
    private final AndroidEmulator emulator;
    private final Module module;
    private final VM vm;
    private final String dirPath = "src/main/resources/demo_resources";


    public BiliSign() {
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.s.sign").build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23)); // 指定android解析库
        vm = emulator.createDalvikVM(new File(dirPath+"/bilibili/apks/bilibili.apk"));

        vm.setJni(this);
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File(dirPath+"/bilibili/libbili.so"), true); // 加载so

        module = dm.getModule(); // 获得so操作橘饼

        dm.callJNI_OnLoad(emulator);
    }

    public String s(TreeMap treeMap) {
        // treeMap :
        // TreeMap<String, String> map = new TreeMap<>();
        //        map.put("ad_extra", "E1133C23F36571A3F1FDE6B325B17419AAD45287455E5292A19CF51300EAF0F2664C808E2C407FBD9E50BD48F8ED17334F4E2D3A07153630BF62F10DC5E53C42E32274C6076A5593C23EE6587F453F57B8457654CB3DCE90FAE943E2AF5FFAE78E574D02B8BBDFE640AE98B8F0247EC0970D2FD46D84B958E877628A8E90F7181CC16DD22A41AE9E1C2B9CB993F33B65E0B287312E8351ADC4A9515123966ACF8031FF4440EC4C472C78C8B0C6C8D5EA9AB9E579966AD4B9D23F65C40661A73958130E4D71F564B27C4533C14335EA64DD6E28C29CD92D5A8037DCD04C8CCEAEBECCE10EAAE0FAC91C788ECD424D8473CAA67D424450431467491B34A1450A781F341ABB8073C68DBCCC9863F829457C74DBD89C7A867C8B619EBB21F313D3021007D23D3776DA083A7E09CBA5A9875944C745BB691971BFE943BD468138BD727BF861869A68EA274719D66276BD2C3BB57867F45B11D6B1A778E7051B317967F8A5EAF132607242B12C9020328C80A1BBBF28E2E228C8C7CDACD1F6CC7500A08BA24C4B9E4BC9B69E039216AA8B0566B0C50A07F65255CE38F92124CB91D1C1C39A3C5F7D50E57DCD25C6684A57E1F56489AE39BDBC5CFE13C540CA025C42A3F0F3DA9882F2A1D0B5B1B36F020935FD64D58A47EF83213949130B956F12DB92B0546DADC1B605D9A3ED242C8D7EF02433A6C8E3C402C669447A7F151866E66383172A8A846CE49ACE61AD00C1E42223");
        //        map.put("appkey", "1d8b6e7d45233436");
        //        map.put("autoplay_card", "11");
        //        map.put("banner_hash", "10687342131252771522");
        //        map.put("build", "6180500");
        //        map.put("c_locale", "zh_CN");
        //        map.put("channel", "shenma117");
        //        map.put("column", "2");
        //        map.put("device_name", "MIX2S");
        //        map.put("device_type", "0");
        //        map.put("flush", "6");
        //        map.put("ts", "1612693177");
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);

        // 由于入参为 sortemap TreeMap实现了SortedMap接口  不好借用unidbg封装方法 可通过native方法来 生成treemap对象
        DvmClass clazz = vm.resolveClass("java/util/Map");
        DvmClass abstractClazz = vm.resolveClass("java/util/AbstractMap", clazz); // 第一个为superClass 第二个为接口class
        DvmObject<?> dvmObject = vm.resolveClass("java/util/TreeMap", abstractClazz).newObject(treeMap);
        list.add(vm.addLocalObject(dvmObject));
        Number number = module.callFunction(emulator, 0x1c97, list.toArray())[0];
        DvmObject result = vm.getObject(number.intValue()); // 返回的是一个对象
        SignedQuery signedQuery = (SignedQuery) result.getValue();
        return signedQuery.b;
    }

    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/util/Map->isEmpty()Z":{
                // TODO
                TreeMap<String, String> treeMap = (TreeMap<String, String>) dvmObject.getValue();
                return treeMap.isEmpty();
            }
        }
        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/util/Map->get(Ljava/lang/Object;)Ljava/lang/Object;":{
                // TODO 获取入参数 进行传递
                TreeMap<String, String> treeMap = (TreeMap<String, String>) dvmObject.getValue();
                String treeKey = (String) varArg.getObjectArg(0).getValue();
                return new StringObject(vm, treeMap.get(treeKey));
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "com/bilibili/nativelibrary/SignedQuery->r(Ljava/util/Map;)Ljava/lang/String;":{
                // TODO 可查jadx 以及hook位置 查看返回值——》避免硬编码 补充jadx中的对应方法
                TreeMap<String, String> treeMap = (TreeMap<String, String>) varArg.getObjectArg(0).getValue();
                String res = BiliTools.r(treeMap);
                return new StringObject(vm, res);
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "com/bilibili/nativelibrary/SignedQuery-><init>(Ljava/lang/String;Ljava/lang/String;)V":{
                // TODO 构造函数 内部做一个简单的内部类 构造这个对象
                String var1 = (String) varArg.getObjectArg(0).getValue();
                String var2 = (String) varArg.getObjectArg(1).getValue();
                return vm.resolveClass("com/bilibili/nativelibrary/SignedQuery").newObject(new SignedQuery(var1, var2));
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }

    class SignedQuery{
        // 补充内部类
        public final String a;
        public final String b;

        SignedQuery(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }
}
