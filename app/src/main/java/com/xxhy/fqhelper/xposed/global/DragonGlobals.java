package com.xxhy.fqhelper.xposed.global;

import android.app.Application;
import com.xxhy.fqhelper.utils.AppUtils;
import org.joor.Reflect;

/**
 * 目标应用全局变量管理类
 * 用于存储和提供目标应用（com.dragon.read）的核心全局对象，如应用实例、类加载器等
 * 采用工具类设计模式，禁止实例化，确保全局访问的唯一性
 */
public class DragonGlobals {
    // 目标应用的类加载器（用于加载目标应用内部类）
    private static ClassLoader classLoader;
    // 目标应用的Application实例（全局上下文）
    private static Application application;
    
    /**
     * 私有构造方法
     * 禁止外部实例化，确保工具类的唯一性
     * @throws UnsupportedOperationException 当尝试实例化时抛出
     */
    private DragonGlobals() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 获取目标应用的Application实例
     * @return 目标应用的Application对象（全局上下文）
     * 说明：优先通过AppUtils获取，若失败则通过反射调用目标应用的AppUtils获取
     */
    public static Application getDragonApplication() {
        if (application == null) {
            try {
                // 尝试通过通用工具类获取应用实例
                application = AppUtils.getApplication();
                // 若获取失败，反射调用目标应用内部的AppUtils获取上下文
                if (application == null) {
                    application = Reflect.onClass(
                            "com.dragon.read.base.util.AppUtils", // 目标应用的AppUtils类
                            getDragonClassLoader() // 目标应用的类加载器
                        )
                        .call("context") // 调用该类的context方法获取应用实例
                        .get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return application;
    }

    /**
     * 设置目标应用的Application实例
     * @param app 目标应用的Application对象
     * 说明：通常在应用初始化完成后（如MainApplication的onCreate后）调用
     */
    public static void setDragonApplication(Application app) {
        application = app;
    }

    /**
     * 初始化目标应用的类加载器
     * @param classLoader 目标应用的ClassLoader实例
     * 说明：在目标应用加载完成时调用，用于后续反射加载目标应用内部类
     */
    public static void initDragonClassLoader(ClassLoader classLoader) {
        DragonGlobals.classLoader = classLoader;
    }

    /**
     * 检查目标应用类加载器是否已初始化
     * @return 若已初始化则返回true，否则返回false
     */
    public static boolean hasInit() {
        return classLoader != null;
    }

    /**
     * 获取目标应用的类加载器
     * @return 已初始化的ClassLoader实例
     * @throws NullPointerException 若类加载器未初始化则抛出
     */
    public static ClassLoader getDragonClassLoader() {
        if (classLoader == null) {
            throw new NullPointerException("ClassLoader has not been initialized");
        }
        return classLoader;
    }
}
