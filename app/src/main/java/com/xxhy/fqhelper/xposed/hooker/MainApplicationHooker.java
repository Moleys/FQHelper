package com.xxhy.fqhelper.xposed.hooker;

import android.app.Application;
import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.utils.NetworkUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.utils.ToastUtils;
import com.xxhy.fqhelper.web.HttpServer;
import com.xxhy.fqhelper.xposed.HookManager;
import com.xxhy.fqhelper.xposed.dexkit.MappingManager;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.io.IOException;
import java.util.Random;

/** 主应用钩子处理器 用于Hook应用主入口类（MainApplication）的初始化方法，在应用启动阶段完成钩子注册、全局初始化等核心操作 */
@XposedHooker
public class MainApplicationHooker implements XposedInterface.Hooker {
  // 随机标识值，用于钩子上下文的区分（多线程环境下隔离不同调用）
  private int magicNumber;

  /**
   * 构造方法
   *
   * @param magic 随机生成的标识值
   */
  public MainApplicationHooker(int magic) {
    this.magicNumber = magic;
  }

  /**
   * 方法调用前的钩子处理
   *
   * @param callback 钩子回调对象，包含调用相关信息
   * @return 当前钩子实例，用于传递上下文到AfterInvocation 说明：在应用初始化前执行关键初始化操作，包括映射表加载和所有钩子的注册
   */
  @BeforeInvocation
  public static MainApplicationHooker beforeInvocation(
      XposedInterface.BeforeHookCallback callback) {

    // 生成随机标识值，用于上下文区分
    int randomKey = new Random().nextInt();
        
    // TODO:需要避免二次加载
        
    // 获取当前应用实例（MainApplication对象），并设置为全局可用
    Application app = (Application) callback.getThisObject();
    ClassLoader cl = app.getClassLoader();
    DragonGlobals.setDragonApplication(app);
    DragonGlobals.initDragonClassLoader(cl);

    // 初始化映射管理（可能用于DexKit的类/方法映射关系加载）
    // MappingManager.initMapping();

    // 注册各类钩子（集中管理所有需要Hook的目标）
    HookManager.hookDragonService(); // 注册下载通知服务钩子
    HookManager.hookSettingActivity(); // 注册设置页面钩子
    HookManager.hookSettingItem(); // 注册设置项钩子
    HookManager.hookAcctManager(); // 注册账户管理钩子
    HookManager.hookPrivilegeManager(); // 注册权限管理钩子
    HookManager.hookUpdateManager(); // 注册更新管理钩子
    HookManager.hookVipInfoModel(); // 注册VIP信息模型钩子，理论应该可以不用hook这个类了，但还是加上保险
    // HookManager.hookNativeLibrary(); // 注册原生库钩子

    return new MainApplicationHooker(randomKey);
  }

  /**
   * 方法调用后的钩子处理
   *
   * @param callback 钩子回调对象，包含方法返回结果等信息
   * @param hookContext 钩子上下文实例（由beforeInvocation返回） 说明：在应用初始化完成后，初始化全局上下文并根据配置启动HTTP服务
   */
  @AfterInvocation
  public static void afterInvocation(
      XposedInterface.AfterHookCallback callback, MainApplicationHooker hookContext) {

    // 读取SP配置，判断是否需要随应用启动HTTP服务
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    if (sp.getBoolean(SPConstants.START_WITH_APP, SPConstants.DEFAULT_START_WITH_APP)) {
      try {
        // 启动HTTP服务（单例模式）
        HttpServer httpServer = HttpServer.getInstance();
        httpServer.start();

        // 显示服务启动成功的提示（包含IP和端口）
        String ipAddress = NetworkUtils.getIPAddress(true); // 获取本地IP地址（true表示优先IPv4）
        String port = sp.getString(SPConstants.PORT, SPConstants.DEFAULT_PORT); // 从配置获取端口
        LogUtils.logI("HTTP Server已启动\n" + ipAddress + ":" + port);
        ToastUtils.show("HTTP Server已启动\n" + ipAddress + ":" + port);
      } catch (IOException e) {
        // 记录服务启动失败的异常日志
        LogUtils.logE("Failed to start HTTP server:", e);
        // 显示启动失败的提示
        ToastUtils.show(e.toString());
      }
    }
  }
}
