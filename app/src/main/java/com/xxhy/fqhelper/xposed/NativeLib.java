package com.xxhy.fqhelper.xposed;

public class NativeLib {
    
    static{
        System.loadLibrary("native-lib");
    }
    
    public static native void doSomething();
}
