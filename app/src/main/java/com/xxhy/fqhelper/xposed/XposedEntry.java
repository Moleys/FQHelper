package com.xxhy.fqhelper.xposed;

import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import com.xxhy.fqhelper.xposed.global.XposedGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * Xposed模块入口类
 * 作为Xposed模块的核心入口，负责初始化模块、处理应用加载事件及提供日志打印功能
 */
public class XposedEntry extends XposedModule {

  // 单例实例，用于全局访问当前Xposed入口对象
  private static XposedEntry instance;
  // Xposed接口对象，用于调用Xposed框架提供的核心功能
  private static XposedInterface xposedInterface;

  /**
   * 构造方法
   * @param baseInterface Xposed框架提供的基础接口
   * @param param 模块加载参数（包含模块相关信息）
   * 说明：初始化单例实例并将自身注册到全局变量，供其他组件访问
   */
  public XposedEntry(XposedInterface baseInterface, XposedModule.ModuleLoadedParam param) {
    super(baseInterface, param);
    instance = this;
    xposedInterface = baseInterface;
    // 将当前实例和模块加载参数存入全局变量，方便其他类获取
    XposedGlobals.setXposedEntryInstance(instance);
    XposedGlobals.setModuleLoadedParam(param);
  }

  /**
   * 应用加载完成时的回调方法
   * @param param 应用加载参数（包含应用包名、类加载器等信息）
   * 说明：当目标应用（com.dragon.read）加载完成后，初始化相关全局变量并启动钩子管理
   */
  @Override
  public void onPackageLoaded(PackageLoadedParam param) {
    super.onPackageLoaded(param);
    // 仅对目标应用"com.dragon.read"进行处理
    if ("com.dragon.read".equals(param.getPackageName())) {
      // 将应用加载参数存入全局变量
      XposedGlobals.setPackageLoadedParam(param);
      // 初始化目标应用的类加载器，供后续反射操作使用
      DragonGlobals.initDragonClassLoader(param.getClassLoader());
      // 启动主应用钩子（会触发其他钩子的注册）
      HookManager.hookMainApplication();
    }
  }

  /**
   * 打印普通信息日志
   * @param message 日志内容
   * 说明：封装父类的日志方法，提供更简洁的调用方式
   */
  public void logI(String message) {
    super.log(message);
  }

  /**
   * 打印错误信息日志
   * @param message 错误描述
   * @param throwable 异常对象
   * 说明：封装父类的错误日志方法，同时记录异常堆栈信息
   */
  public void logE(String message, Throwable throwable) {
    super.log(message, throwable);
  }
}
