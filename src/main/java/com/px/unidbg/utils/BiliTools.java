package com.px.unidbg.utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class BiliTools {
    // 为防止编码不同 从jadx补充此位置的方法
    private static final char[] f14934c = "0123456789ABCDEF".toCharArray();
    public static final String KEY_VALUE_DELIMITER = "=";
    public static final String FIELD_DELIMITER = "&";

    private static boolean a(char c2, String str) {
        return (c2 >= 'A' && c2 <= 'Z') || (c2 >= 'a' && c2 <= 'z') || !((c2 < '0' || c2 > '9') && "-_.~".indexOf(c2) == -1 && (str == null || str.indexOf(c2) == -1));
    }

    static String c(String str, String str2) {
        StringBuilder sb = null;
        if (str == null) {
            return null;
        }
        int length = str.length();
        int i2 = 0;
        while (i2 < length) {
            int i3 = i2;
            while (i3 < length && a(str.charAt(i3), str2)) {
                i3++;
            }
            if (i3 != length) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                if (i3 > i2) {
                    sb.append((CharSequence) str, i2, i3);
                }
                i2 = i3 + 1;
                while (i2 < length && !a(str.charAt(i2), str2)) {
                    i2++;
                }
                byte[] bytes = str.substring(i3, i2).getBytes(StandardCharsets.UTF_8);
                int length2 = bytes.length;
                for (int i4 = 0; i4 < length2; i4++) {
                    sb.append('%');
                    sb.append(f14934c[(bytes[i4] & 240) >> 4]);
                    sb.append(f14934c[bytes[i4] & 15]);
                }
            } else if (i2 == 0) {
                return str;
            } else {
                sb.append((CharSequence) str, i2, length);
                return sb.toString();
            }
        }
        return sb == null ? str : sb.toString();
    }

    static String b(String str) {
        return c(str, null);
    }

    public static String r(Map<String, String> map){
        String str;
        if (!(map instanceof SortedMap)) {
            map = new TreeMap(map);
        }
        StringBuilder sb = new StringBuilder(256);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!key.isEmpty()) { // 改 TextUtils -> android判断为空
                sb.append(b(key));
                sb.append(KEY_VALUE_DELIMITER);
                String value = entry.getValue();
                if (value == null) {
                    str = "";
                } else {
                    str = b(value);
                }
                sb.append(str);
                sb.append(FIELD_DELIMITER);
            }
        }
        int length = sb.length();
        if (length > 0) {
            sb.deleteCharAt(length - 1);
        }
        if (length == 0) {
            return null;
        }
        return sb.toString();
    }
}
