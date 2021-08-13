// 注入 当前应用的js frida -UF -l xx.js
function hookSign(){
	// hook sign 入参 和 结果
	Java.perform(function(){
		var className = "com.bilibili.nativelibrary.LibBili";
		var bili = Java.use(className);
		var targetMethod = "s";
		bili[targetMethod].implementation = function(){
			var map = arguments[0];
			// 打印入参
			console.log("\n>>输出map信息：", map.entrySet().toArrary());
			// 还原方法
			var result = this[targetMethod](arguments[0]);
			console.log("\n>>输出执行结果：", result);
			return result;
		}
	});
}

function callSign(){
	// 主动调用
	Java.perform(function(){
		var className = "com.bilibili.nativelibrary.LibBili";
		var bili = Java.use(className);
		var treeMap = Java.use("java.util.TreeMap");
		var map = treeMap.$new();
		map.put("ad_extra", "E1133C23F36571A3F1FDE6B325B17419AAD45287455E5292A19CF51300EAF0F2664C808E2C407FBD9E50BD48F8ED17334F4E2D3A07153630BF62F10DC5E53C42E32274C6076A5593C23EE6587F453F57B8457654CB3DCE90FAE943E2AF5FFAE78E574D02B8BBDFE640AE98B8F0247EC0970D2FD46D84B958E877628A8E90F7181CC16DD22A41AE9E1C2B9CB993F33B65E0B287312E8351ADC4A9515123966ACF8031FF4440EC4C472C78C8B0C6C8D5EA9AB9E579966AD4B9D23F65C40661A73958130E4D71F564B27C4533C14335EA64DD6E28C29CD92D5A8037DCD04C8CCEAEBECCE10EAAE0FAC91C788ECD424D8473CAA67D424450431467491B34A1450A781F341ABB8073C68DBCCC9863F829457C74DBD89C7A867C8B619EBB21F313D3021007D23D3776DA083A7E09CBA5A9875944C745BB691971BFE943BD468138BD727BF861869A68EA274719D66276BD2C3BB57867F45B11D6B1A778E7051B317967F8A5EAF132607242B12C9020328C80A1BBBF28E2E228C8C7CDACD1F6CC7500A08BA24C4B9E4BC9B69E039216AA8B0566B0C50A07F65255CE38F92124CB91D1C1C39A3C5F7D50E57DCD25C6684A57E1F56489AE39BDBC5CFE13C540CA025C42A3F0F3DA9882F2A1D0B5B1B36F020935FD64D58A47EF83213949130B956F12DB92B0546DADC1B605D9A3ED242C8D7EF02433A6C8E3C402C669447A7F151866E66383172A8A846CE49ACE61AD00C1E42223");
        map.put("appkey", "1d8b6e7d45233436");
        map.put("autoplay_card","11");
        map.put("banner_hash","10687342131252771522");
        map.put("build","6180500");
        map.put("c_locale","zh_CN");
        map.put("channel","shenma117");
        map.put("column","2");
        map.put("device_name","MIX2S");
        map.put("device_type","0");
        map.put("flush","6");
        map.put("ts","1612693177");

        var result = bili.s(map);
        console.log("\n 输出结果:", result);
        return result;
	});
}

// 一共出现 7次md5 观察 执行sign生成只有5次 第一次为明文加密 后四次为 固定参数生成
// -》 sign加密规则：md5(明文字符串拼接 + 560c52cc + d288fed0 + 45859ed1 + 8bffd973)
function callMd5Update(){
	// 对 so中疑似 md5加密的地方进行 native hook
	var libName = "libbili.so";
	var lib = Module.findBaseAddress(libName);
	if(lib){
		// sub_22B0为md5update方法 由于是thumb 模式 地址偏移量需要 +1
		var md5_update = lib.add(0x22b0 + 1);
		Interceptor.attach(md5_update, {
			onEnter: function(args){
				console.log("\n 输出内容:");
				// args[1]为mdtupdate 加密的明文 args[2]为长度  指定 hexdump的长度
				console.log(hexdump(args[1], {length: args[2].toInt32()}));
				console.log("\n 长度:" + args[2]);

			},
			onLeave: function(args){

			}
		})

	}

}



