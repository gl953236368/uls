// com.ss.android.auto
function hookTfcc(){
	// 确定参数 v1:每次调用 +1/ v2:固定为1 / 
	// v3: 固定值14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8= / v4:请求包中需要解密的字符串
	console.log("enter function");
	Java.perform(function(){
		var tfcc = Java.use("com.bdcaijing.tfccsdk.Tfcc");
		tfcc.tfccDecrypt.implementation = function(v1, v2, v3, v4){
			console.log(">>>>>>>decode>>>>>>>>>");	
			console.log("param1:",v1);
			console.log("param2:",v2);
			console.log("param3:",v3);
			console.log("param3:",v4);
			var ret = this.tfccDecrypt(v1, v2, v3, v4);
			console.log("resutl:", ret);
			console.log(">>>>>>>>>>>>>>>>>>>>>");
			return ret;
		};

		tfcc.tfccEncrypt.implementation = function(v1, v2, v3, v4){
			console.log(">>>>>>>>encode>>>>>>>>>>>");
			console.log("param1:",v1);
			console.log("param2:",v2);
			console.log("param3:",v3);
			console.log("param3:",v4);
			var ret = this.tfccEncrypt(v1, v2, v3, v4);
			console.log("resutl:", ret);
			console.log(">>>>>>>>>>>>>>>>>>>>>");
			return ret;
		}
	})

}

function activeTFCC(){
	Java.perform(function(){
		var v1  = 1;
		var v2 = 10;
		var v3 = "14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8=";
		var v4 = "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";
		var tfcc = Java.use("com.bdcaijing.tfccsdk.Tfcc").$new();
		var result = tfcc.tfccDecrypt(v1, v2, v3, v4);
		console.log("主动调用结果:",result);

	})
}

// 定位 so位置
// [RegisterNatives] java_class: com.bdcaijing.tfccsdk.Tfcc name: tfccDecrypt sig: 
// (IILjava/lang/String;Ljava/lang/String;)Ljava/lang/String; 
// fnPtr: 0xe2ea32ac module_name: libcjtfcc.so module_base: 0xe2e99000 offset: 0xa2ac
function hook_RegisterNatives() {
    var symbols = Module.enumerateSymbolsSync("libart.so");
    var addrRegisterNatives = null;
    for (var i = 0; i < symbols.length; i++) {
        var symbol = symbols[i];
        
        //_ZN3art3JNI15RegisterNativesEP7_JNIEnvP7_jclassPK15JNINativeMethodi
        if (symbol.name.indexOf("art") >= 0 &&
                symbol.name.indexOf("JNI") >= 0 && 
                symbol.name.indexOf("RegisterNatives") >= 0 && 
                symbol.name.indexOf("CheckJNI") < 0) {
            addrRegisterNatives = symbol.address;
            console.log("RegisterNatives is at ", symbol.address, symbol.name);
        }
    }

    if (addrRegisterNatives != null) {
        Interceptor.attach(addrRegisterNatives, {
            onEnter: function (args) {
                console.log("[RegisterNatives] method_count:", args[3]);
                var env = args[0];
                var java_class = args[1];
                var class_name = Java.vm.tryGetEnv().getClassName(java_class);
                //console.log(class_name);

                var methods_ptr = ptr(args[2]);

                var method_count = parseInt(args[3]);
                for (var i = 0; i < method_count; i++) {
                    var name_ptr = Memory.readPointer(methods_ptr.add(i * Process.pointerSize * 3));
                    var sig_ptr = Memory.readPointer(methods_ptr.add(i * Process.pointerSize * 3 + Process.pointerSize));
                    var fnPtr_ptr = Memory.readPointer(methods_ptr.add(i * Process.pointerSize * 3 + Process.pointerSize * 2));

                    var name = Memory.readCString(name_ptr);
                    var sig = Memory.readCString(sig_ptr);
                    var find_module = Process.findModuleByAddress(fnPtr_ptr);
                    console.log("[RegisterNatives] java_class:", class_name, "name:", name, "sig:", sig, "fnPtr:", fnPtr_ptr, "module_name:", find_module.name, "module_base:", find_module.base, "offset:", ptr(fnPtr_ptr).sub(find_module.base));

                }
            }
        });
    }
}

// setImmediate(hook_RegisterNatives);

// setImmediate(hookTfcc);