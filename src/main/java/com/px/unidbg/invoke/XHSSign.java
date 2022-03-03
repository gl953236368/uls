package com.px.unidbg.invoke;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XHSSign extends AbstractJni{
    /**
     * Template: 小红书 6.97.01.apk  -> okhttp3.interceptor
     * Content: 主要填补环境，需要部分来自手机 文件s.xml的值
     * Method: unidbg补充环境
     * Algorithm: md5魔改
     */
    private final AndroidEmulator androidEmulator;
    private final VM vm;
    private final Module module;
    private Request request = null;
    private Map<String, String> headers = new HashMap<>();
    String mainHmac;
    String deviceId;
    private final String dirPath = "src/main/resources/demo_resources";


    public XHSSign() {


        androidEmulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.xhs.shield").build(); // 自定义 模拟器
        final Memory memory = androidEmulator.getMemory(); // 模拟器内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        vm = androidEmulator.createDalvikVM(new File(dirPath+"/xhs/apks/xhs_6.97.01.apk"));
        vm.setVerbose(false);

        DalvikModule dalvikModule = vm.loadLibrary(new File(dirPath+ "/xhs/libshield6.97.so"), true);
        vm.setJni(this);
        module = dalvikModule.getModule();

        dalvikModule.callJNI_OnLoad(androidEmulator);
    }

    public void callInitializeNative(){
        // offset 是基于apk内部内存偏移量 .so中参照 data段 对应方法 DCD
        // 初始化函数
        List<Object> objectList = new ArrayList<>(10);
        objectList.add(vm.getJNIEnv());  // 参数 env
        objectList.add(0); // jclass/jobject 直接填0
        module.callFunction(androidEmulator, 0x6c11d, objectList.toArray());
    }

    public long callInitialize(){
        // 第二个函数
        List<Object> objectList = new ArrayList<>(10);
        objectList.add(vm.getJNIEnv());
        objectList.add(0);
        objectList.add(vm.addLocalObject(new StringObject(vm, "main")));
        Number number = module.callFunction(androidEmulator, 0x6b801, objectList.toArray())[0];
        return number.longValue();
    }

    public String callInterceptor(long ptr){
        List<Object> objectList = new ArrayList<>(10);
        objectList.add(vm.getJNIEnv());
        objectList.add(0);
        DvmObject<?> chain = vm.resolveClass("okhttp3/Interceptor$Chain").newObject(null);
        objectList.add(vm.addLocalObject(chain));
        objectList.add(ptr);
        module.callFunction(androidEmulator, 0x6b9e9, objectList.toArray());
        return this.headers.get("shield");
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            case "com/xingin/shield/http/ContextHolder->sLogger:Lcom/xingin/shield/http/ShieldLogger;":{
                return vm.resolveClass("com/xingin/shield/http/ShieldLogger").newObject(signature);
            }
            case "com/xingin/shield/http/ContextHolder->sDeviceId:Ljava/lang/String;":{
                return new StringObject(vm, deviceId);
            }
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "com/xingin/shield/http/ShieldLogger->nativeInitializeStart()V":
            case "com/xingin/shield/http/ShieldLogger->nativeInitializeEnd()V":
            case "com/xingin/shield/http/ShieldLogger->initializeStart()V":
            case "com/xingin/shield/http/ShieldLogger->initializedEnd()V":
            case "com/xingin/shield/http/ShieldLogger->buildSourceStart()V":
            case "com/xingin/shield/http/ShieldLogger->buildSourceEnd()V":
            case "com/xingin/shield/http/ShieldLogger->calculateStart()V":
            case "com/xingin/shield/http/ShieldLogger->calculateEnd()V": {
                return;
            }
            case "okhttp3/RequestBody->writeTo(Lokio/BufferedSink;)V": {
                BufferedSink bufferedSink = (BufferedSink) vaList.getObjectArg(0).getValue();
                RequestBody requestBody = (RequestBody) dvmObject.getValue();
                if(requestBody != null){
                    try {
                        requestBody.writeTo(bufferedSink);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                return;
            }
        }

        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "java/nio/charset/Charset->defaultCharset()Ljava/nio/charset/Charset;":{
                return vm.resolveClass("java/nio/charset/Charset").newObject(Charset.defaultCharset());
            }
            case "com/xingin/shield/http/Base64Helper->decode(Ljava/lang/String;)[B":{
                String input = (String) vaList.getObjectArg(0).getValue();
                byte[] result = Base64.decodeBase64(input);
                return new ByteArray(vm, result);
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature){
            case "android/content/pm/PackageInfo->versionCode:I": {
                return 6970181;
            }
        }
        return super.getIntField(vm, dvmObject, signature);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            case "com/xingin/shield/http/ContextHolder->sAppId:I":{
                return -319115519;
            }
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "android/content/Context->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;":
                return vm.resolveClass("android/content/SharedPreferences").newObject(vaList.getObjectArg(0));
            case "android/content/SharedPreferences->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;": {
                if(((StringObject) dvmObject.getValue()).getValue().equals("s")){
                    if (vaList.getObjectArg(0).getValue().equals("main")) {
                        return new StringObject(vm, "");
                    }
                    if(vaList.getObjectArg(0).getValue().equals("main_hmac")){
                        return  new StringObject(vm, mainHmac);
                    }
                }
            }
            case "okhttp3/Interceptor$Chain->request()Lokhttp3/Request;": {
                DvmClass clazz = vm.resolveClass("okhttp3/Request");
                return clazz.newObject(request);
            }
            case "okhttp3/Request->url()Lokhttp3/HttpUrl;": {
                DvmClass clazz = vm.resolveClass("okhttp3/HttpUrl");
                Request request = (Request) dvmObject.getValue();
                return clazz.newObject(request.url());
            }
            case "okhttp3/HttpUrl->encodedPath()Ljava/lang/String;": {
                HttpUrl httpUrl = (HttpUrl) dvmObject.getValue();
                return new StringObject(vm, httpUrl.encodedPath());
            }
            case "okhttp3/HttpUrl->encodedQuery()Ljava/lang/String;": {
                HttpUrl httpUrl = (HttpUrl) dvmObject.getValue();
                return new StringObject(vm, httpUrl.encodedQuery());
            }
            case "okhttp3/Request->body()Lokhttp3/RequestBody;": {
                Request request = (Request) dvmObject.getValue();
                return vm.resolveClass("okhttp3/RequestBody").newObject(request.body());
            }
            case "okhttp3/Request->headers()Lokhttp3/Headers;": {
                Request request = (Request) dvmObject.getValue();
                return vm.resolveClass("okhttp3/Headers").newObject(request.headers());
            }
            case "okio/Buffer->writeString(Ljava/lang/String;Ljava/nio/charset/Charset;)Lokio/Buffer;": {
                Buffer buffer = (Buffer) dvmObject.getValue();
                buffer.writeString(vaList.getObjectArg(0).getValue().toString(), (Charset) vaList.getObjectArg(1).getValue());
                return dvmObject;
            }
            case "okhttp3/Headers->name(I)Ljava/lang/String;": {
                Headers headers = (Headers) dvmObject.getValue();
                return new StringObject(vm, headers.name(vaList.getIntArg(0)));
            }
            case "okhttp3/Headers->value(I)Ljava/lang/String;": {
                Headers headers = (Headers) dvmObject.getValue();
                return new StringObject(vm, headers.value(vaList.getIntArg(0)));
            }
            case "okio/Buffer->clone()Lokio/Buffer;": {
                Buffer buffer = (Buffer) dvmObject.getValue();
                return vm.resolveClass("okio/Buffer").newObject(buffer.clone());
            }
            case "okhttp3/Request->newBuilder()Lokhttp3/Request$Builder;": {
                Request request = (Request) dvmObject.getValue();
                return vm.resolveClass("okhttp3/Request$Builder").newObject(request.newBuilder());
            }
            case "okhttp3/Request$Builder->header(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Request$Builder;": {
                Request.Builder builder = (Request.Builder) dvmObject.getValue();
                builder.header(vaList.getObjectArg(0).getValue().toString(), vaList.getObjectArg(1).getValue().toString());
                if(vaList.getObjectArg(0).getValue().equals("shield")){
                    this.headers.put("shield", vaList.getObjectArg(1).getValue().toString());
                }
                return dvmObject;
            }
            case "okhttp3/Request$Builder->build()Lokhttp3/Request;": {
                Request.Builder builder = (Request.Builder) dvmObject.getValue();
                return vm.resolveClass("okhttp3/Request").newObject(builder.build());
            }
            case "okhttp3/Interceptor$Chain->proceed(Lokhttp3/Request;)Lokhttp3/Response;": {
                return vm.resolveClass("okhttp3/Response").newObject(null);
            }
        }

        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "okio/Buffer-><init>()V":
                return dvmClass.newObject(new Buffer());
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "okhttp3/Headers->size()I":
                Headers headers = (Headers) dvmObject.getValue();
                return headers.size();
            case "okhttp3/Response->code()I":
                return 200;
            case "okio/Buffer->read([B)I":
                Buffer buffer = (Buffer) dvmObject.getValue();
                return buffer.read((byte[]) vaList.getObjectArg(0).getValue());
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }


    public String main(String deviceID, String main_hmac, String absoluteUrl, Map<String,String>headerMap) {
//        long bgTime = System.currentTimeMillis() / 1000;
        // 生成参数
        request = new Request.Builder()
                .url(absoluteUrl)
                .addHeader("x-b3-traceid", headerMap.get("x-b3-traceid"))
                .addHeader("xy-common-params", headerMap.get("xy-common-params"))
                .addHeader("user-agent", headerMap.get("user-agent"))
                .build();
        mainHmac = main_hmac;
        deviceId = deviceID;
        callInitializeNative();
        long ptr = callInitialize();
        String result = callInterceptor(ptr);

//        System.out.println("耗时："+(System.currentTimeMillis() / 1000 - bgTime));

        return result;
    }
}
