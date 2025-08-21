package com.xxhy.fqhelper.xposed.global;

import com.xxhy.fqhelper.xposed.XposedEntry;
import io.github.libxposed.api.XposedModule;

/**
 * Xposed全局变量管理类
 * 用于存储和提供Xposed模块运行过程中的核心全局对象，如Xposed入口实例、模块加载参数等
 * 采用工具类设计模式，禁止实例化，确保全局访问的唯一性
 */
public class XposedGlobals {
    
    // Xposed模块入口实例（单例）
    private static XposedEntry xposedEntryInstance;
    // 模块加载参数（包含模块自身信息）
    private static XposedModule.ModuleLoadedParam moduleLoadedParam;
    // 应用包加载参数（包含目标应用的包信息、类加载器等）
    private static XposedModule.PackageLoadedParam packageLoadedParam;
    
    /**
     * 私有构造方法
     * 禁止外部实例化，确保工具类的唯一性
     * @throws UnsupportedOperationException 当尝试实例化时抛出
     */
    private XposedGlobals() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 设置Xposed入口实例
     * @param instance XposedEntry实例（通常在XposedEntry构造时调用）
     */
    public static void setXposedEntryInstance(XposedEntry instance){
        xposedEntryInstance = instance;
    }
    
    /**
     * 获取Xposed入口实例
     * @return 已初始化的XposedEntry实例
     * @throws NullPointerException 若实例未初始化则抛出
     */
    public static XposedEntry getXposedEntryInstance(){
        if (xposedEntryInstance == null) {
			throw new NullPointerException("XposedEntryInstance has not been initialized");
		}
        return xposedEntryInstance;
    }
    
    /**
     * 设置模块加载参数
     * @param param 模块加载参数（通常在XposedEntry构造时调用）
     */
    public static void setModuleLoadedParam(XposedModule.ModuleLoadedParam param){
        moduleLoadedParam = param;
    }
    
    /**
     * 获取模块加载参数
     * @return 已初始化的模块加载参数
     * @throws NullPointerException 若参数未初始化则抛出
     */
    public static XposedModule.ModuleLoadedParam getModuleLoadedParam(){
        if (moduleLoadedParam == null) {
			throw new NullPointerException("ModuleLoadedParam has not been initialized");
		}
        return moduleLoadedParam;
    }
    
    /**
     * 设置应用包加载参数
     * @param param 应用包加载参数（通常在目标应用加载完成时调用）
     */
    public static void setPackageLoadedParam(XposedModule.PackageLoadedParam param){
        packageLoadedParam = param;
    }
    
    /**
     * 获取应用包加载参数
     * @return 已初始化的应用包加载参数
     * @throws NullPointerException 若参数未初始化则抛出
     */
    public static XposedModule.PackageLoadedParam getPackageLoadedParam(){
        if (packageLoadedParam == null) {
			throw new NullPointerException("PackageLoadedParam has not been initialized");
		}
        return packageLoadedParam;
    }
    
}
