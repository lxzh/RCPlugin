package com.geetest

import java.text.SimpleDateFormat;

public class Log {
    private static final int VERBOSE = 1;
    private static final int DEBUG = 2;
    private static final int INFO = 3;
    private static final int WARN = 4;
    private static final int ERROR = 5;
    private static final int NOTHING = 6;

    private static final String V = "V";
    private static final String D = "D";
    private static final String I = "I";
    private static final String W = "W";
    private static final String E = "E";
    private static final String N = "N";

    private static int LEVEL = VERBOSE;
    private static final String DEFAULT_TAG = "RcPlugin";
    private static String TAG = DEFAULT_TAG;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//    private static final byte[] SYSTEM_CLASS_NAME = new byte[]{0x6a, 0x61, 0x76, 0x61, 0x2e, 0x6c, 0x61, 0x6e, 0x67, 0x2e, 0x53, 0x79, 0x73, 0x74, 0x65, 0x6D};// "java.lang.System"
//    private static final byte[] OUT_FIELD_NAME = new byte[]{0x6F, 0x75, 0x74};                               // "out"
//    private static final byte[] PRINTLN_METHOD_NAME = new byte[]{0x70, 0x72, 0x69, 0x6E, 0x74, 0x6C, 0x6E};  // "println"

    private static boolean isEnable = false;
    private static Object SYSTEM_OUT;
    private static java.lang.reflect.Method PRINTLN_METHOD;

    public static void init(int level) {
        LEVEL = level;
    }

//    public static void openLog(String methodName) { // println
//        openLog(methodName, DEFAULT_TAG);
//    }
//
//    public static void openLog(String methodName, String tag) { // println
//        Class<?> systemClass = null;
//        try {
//            systemClass = Class.forName(new String(SYSTEM_CLASS_NAME));
//            java.lang.reflect.Field systemOutField = systemClass.getDeclaredField(new String(OUT_FIELD_NAME));
//            SYSTEM_OUT = systemOutField.get(systemClass);
//            PRINTLN_METHOD = SYSTEM_OUT.getClass().getMethod(methodName, String.class);
//            isEnable = true;
//            TAG = tag;
//        } catch (Exception e) {
//
//        }
//    }

    public static void v(String msg) {
        v(TAG, msg);
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void i(String msg) {
        i(TAG, msg);
    }

    public static void w(String msg) {
        w(TAG, msg);
    }

    public static void e(String msg) {
        e(TAG, msg);
    }

    public static void fe(String msg) {
        fe(TAG, msg);
    }

    public static void v(String tag, String msg) {
        if (LEVEL <= VERBOSE) {
            log(V, tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (LEVEL <= DEBUG) {
            log(D, tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (LEVEL <= INFO) {
            log(I, tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (LEVEL <= WARN) {
            log(W, tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (LEVEL <= ERROR) {
            log(E, tag, msg);
        }
    }

    public static void fe(String tag, String msg) {
        log(E, tag, msg);
    }

    private static void log(String level, String tag, String msg) {
//        if (!isEnable || SYSTEM_OUT == null || PRINTLN_METHOD == null) {
//            return;
//        }
        Date date = new Date();
        StringBuilder sb = new StringBuilder();
        sb.append(sdf.format(date))
        sb.append(" >>> ")
        sb.append(tag);
        sb.append(" [");
        sb.append(level);
        sb.append("] ");
        sb.append(msg);
        System.out.println(sb.toString());
//        try {
//            PRINTLN_METHOD.invoke(SYSTEM_OUT, sb.toString());
//        } catch (Exception e) {
//
//        }
    }
}
