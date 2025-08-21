package com.xxhy.fqhelper.xposed;

import android.view.View;
import com.xxhy.fqhelper.xposed.global.XposedGlobals;
import com.xxhy.fqhelper.xposed.hooker.*;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Hook管理类，负责统一管理所有目标类和方法的挂钩逻辑 集中处理Xposed模块对目标应用各类组件的Hook操作，包括Activity、Service、Manager等 */
public class HookManager {

  // region 常量定义 - 目标类名（提取硬编码类名作为常量，便于维护）
  private static final String SETTINGS_ACTIVITY_CLASS =
      "com.dragon.read.component.biz.impl.mine.settings.SettingsActivity";
  private static final String RECYCLER_VIEW_ADAPTER_CLASS = "com.dragon.read.recyler.c";
  private static final String SETTING_ITEM_LISTENER_CLASS =
      "com.dragon.read.component.biz.impl.mine.settings.item.d$a";
  private static final String ITEM_MODEL_CLASS = "j03.c";
  private static final String MAIN_APPLICATION_CLASS = "com.dragon.read.app.MainApplication";
  private static final String DOWNLOAD_SERVICE_CLASS =
      "com.ss.android.socialbase.downloader.notification.DownloadNotificationService";
  private static final String UPDATE_LISTENER_CLASS =
      "com.ss.android.update.OnUpdateStatusChangedListener";
  private static final String UPDATE_MANAGER_CLASS = "com.ss.android.update.UpdateServiceImpl";
  private static final String VIP_COMMON_SUB_TYPE_CLASS =
      "com.dragon.read.rpc.model.VipCommonSubType";
  private static final String VIP_INFO_MODEL_CLASS = "com.dragon.read.user.model.VipInfoModel";
  private static final String ACCT_MANAGER_CLASS = "com.dragon.read.user.AcctManager";
  private static final String PRIVILEGE_MANAGER_CLASS =
      "com.dragon.read.component.biz.impl.privilege.PrivilegeManager";
  // endregion

  // Xposed入口实例（用于执行Hook操作）
  private static final XposedEntry xposedEntry = XposedGlobals.getXposedEntryInstance();
  // 目标应用加载参数（包含类加载器等信息）
  private static final XposedModule.PackageLoadedParam packageLoadedParam =
      XposedGlobals.getPackageLoadedParam();

  /**
   * 挂钩设置界面Activity（SettingsActivity） 作用：修改设置页面的展示逻辑，通过Hook其I1和J1方法实现，由SettingActivityHooker处理具体逻辑
   */
  public static void hookSettingActivity() {
    try {
      // 获取目标应用的类加载器
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      // 加载目标类
      Class<?> settingActivityClass = loadTargetClass(targetClassLoader, SETTINGS_ACTIVITY_CLASS);
      Class<?> recyclerViewAdapterClass =
          loadTargetClass(targetClassLoader, RECYCLER_VIEW_ADAPTER_CLASS);

      // 获取目标方法（N1和O1，参数为RecyclerView适配器类）
      Method method1 = settingActivityClass.getDeclaredMethod("N1", recyclerViewAdapterClass);
      Method method2 = settingActivityClass.getDeclaredMethod("O1", recyclerViewAdapterClass);

      // 执行挂钩，使用SettingActivityHooker处理
      xposedEntry.hook(method1, SettingActivityHooker.class);
      xposedEntry.hook(method2, SettingActivityHooker.class);
    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookSettingActivity] 挂钩设置界面失败！", t);
    }
  }

  /** 挂钩设置项点击监听器（SettingItemListener） 作用：修改设置项的点击事件逻辑，通过Hook其a方法实现，由SettingItemHooker处理具体逻辑 */
  public static void hookSettingItem() {
    try {
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      Class<?> settingItemListenerClass =
          loadTargetClass(targetClassLoader, SETTING_ITEM_LISTENER_CLASS);
      Class<?> itemModelClass = loadTargetClass(targetClassLoader, ITEM_MODEL_CLASS);

      // 获取目标方法（a，参数为View、Item模型、位置索引）
      Method method =
          settingItemListenerClass.getDeclaredMethod("a", View.class, itemModelClass, int.class);

      xposedEntry.hook(method, SettingItemHooker.class);
    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookSettingItem] 挂钩设置项监听器失败！", t);
    }
  }

  /**
   * 挂钩主应用类（MainApplication） 作用：监控应用启动初始化过程，通过Hook其onCreateAlways方法实现，由MainApplicationHooker处理具体逻辑
   */
  public static void hookMainApplication() {
    try {
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      Class<?> mainApplicationClass = loadTargetClass(targetClassLoader, MAIN_APPLICATION_CLASS);

      // 获取应用初始化方法
      Method onCreateMethod = mainApplicationClass.getDeclaredMethod("onCreateAlways");

      xposedEntry.hook(onCreateMethod, MainApplicationHooker.class);
    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookMainApplication] 挂钩主应用类失败！", t);
    }
  }

  /**
   * 挂钩下载通知服务（DownloadNotificationService）
   * 作用：修改下载通知的展示逻辑，通过Hook其onCreate方法实现，由DragonServiceHooker处理具体逻辑
   */
  public static void hookDragonService() {
    try {
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      Class<?> downloadServiceClass = loadTargetClass(targetClassLoader, DOWNLOAD_SERVICE_CLASS);

      // 获取服务创建方法
      Method onCreateMethod = downloadServiceClass.getDeclaredMethod("onCreate");

      xposedEntry.hook(onCreateMethod, DragonServiceHooker.class);
    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookDragonService] 挂钩下载通知服务失败！", t);
    }
  }

  /** 挂钩更新管理器（UpdateManager） 作用：拦截应用更新检查逻辑，通过Hook其多个checkUpdate重载方法实现，由UpdateManagerHooker处理具体逻辑 */
  public static void hookUpdateManager() {
    try {
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      Class<?> updateListenerClass = loadTargetClass(targetClassLoader, UPDATE_LISTENER_CLASS);
      Class<?> updateManagerClass = loadTargetClass(targetClassLoader, UPDATE_MANAGER_CLASS);

      // 获取多个重载的更新检查方法
      Method checkUpdate1 =
          updateManagerClass.getDeclaredMethod(
              "checkUpdate", int.class, int.class, updateListenerClass, boolean.class);
      Method checkUpdate2 =
          updateManagerClass.getDeclaredMethod("checkUpdate", int.class, updateListenerClass);
      Method checkUpdate3 =
          updateManagerClass.getDeclaredMethod(
              "checkUpdate", int.class, updateListenerClass, boolean.class);

      xposedEntry.hook(checkUpdate1, UpdateManagerHooker.class);
      xposedEntry.hook(checkUpdate2, UpdateManagerHooker.class);
      xposedEntry.hook(checkUpdate3, UpdateManagerHooker.class);
    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookUpdateManager] 挂钩更新管理器失败！", t);
    }
  }

  /** 挂钩VIP信息模型（VipInfoModel） 作用：修改VIP信息的初始化逻辑，通过Hook其构造方法实现，由VipInfoModelHooker处理具体逻辑 */
  public static void hookVipInfoModel() {
    try {
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      Class<?> vipSubTypeClass = loadTargetClass(targetClassLoader, VIP_COMMON_SUB_TYPE_CLASS);
      Class<?> vipInfoModelClass = loadTargetClass(targetClassLoader, VIP_INFO_MODEL_CLASS);

      // 获取目标构造方法（参数包含多个VIP相关属性）
      Constructor<?> constructor =
          vipInfoModelClass.getConstructor(
              String.class,
              String.class,
              String.class,
              boolean.class,
              boolean.class,
              int.class,
              boolean.class,
              vipSubTypeClass);

      xposedEntry.hook(constructor, VipInfoModelHooker.class);
    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookVipInfoModel] 挂钩VIP信息模型失败！", t);
    }
  }

  /** 挂钩账户管理器（AcctManager） 作用：修改账户信息相关逻辑，通过Hook其M方法实现，由AcctManagerHooker处理具体逻辑 */
  public static void hookAcctManager() {
    try {
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      Class<?> acctManagerClass = loadTargetClass(targetClassLoader, ACCT_MANAGER_CLASS);

      // 获取目标方法（M方法，具体功能需结合业务逻辑）
      Method methodM = acctManagerClass.getDeclaredMethod("M");

      xposedEntry.hook(methodM, AcctManagerHooker.class);
    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookAcctManager] 挂钩账户管理器失败！", t);
    }
  }

  /** 挂钩权限管理器（PrivilegeManager） 作用：修改VIP权限及广告相关逻辑，通过Hook多个权限判断方法实现，由PrivilegeManagerHooker处理具体逻辑 */
  public static void hookPrivilegeManager() {
    try {
      ClassLoader targetClassLoader = packageLoadedParam.getClassLoader();
      Class<?> privilegeManagerClass = loadTargetClass(targetClassLoader, PRIVILEGE_MANAGER_CLASS);

      // 挂钩多个权限判断方法（VIP状态、广告权限等），大部分均为 native 方法，不知道只拦截 Java 层会不会遗漏
      // TODO:在Native层拦截这些方法

      // canShowVipRelational影响太大，出版书vip和非vip无法区分
      Method method1 = privilegeManagerClass.getDeclaredMethod("canShowVipRelational");

      Method method2 = privilegeManagerClass.getDeclaredMethod("hasNoAdFollAllScene");
      Method method3 = privilegeManagerClass.getDeclaredMethod("hasNoAdForShortSeries");
      Method method4 = privilegeManagerClass.getDeclaredMethod("hasNoAdPrivilege");
      Method method5 = privilegeManagerClass.getDeclaredMethod("hasNoAdReadConsumptionPrivilege");
      Method method6 = privilegeManagerClass.getDeclaredMethod("isForeverNoAd");
      Method method7 = privilegeManagerClass.getDeclaredMethod("isNoAd", String.class);
      Method method8 = privilegeManagerClass.getDeclaredMethod("isVip");
      Method method9 = privilegeManagerClass.getDeclaredMethod("showPayVipEntranceInChapterEnd");
      Method method10 = privilegeManagerClass.getDeclaredMethod("hasPrivilege", String.class);
      Method method11 = privilegeManagerClass.getDeclaredMethod("getInstance");
      Method method12 =
          privilegeManagerClass.getDeclaredMethod(
              "updateVipInfo",
              targetClassLoader.loadClass("com.dragon.read.user.model.VipInfoModel"),
              boolean.class);
      Method method13 = privilegeManagerClass.getDeclaredMethod("isBookAdFree", String.class);

      xposedEntry.hook(method1, PrivilegeManagerHooker.class);
      xposedEntry.hook(method2, PrivilegeManagerHooker.class);
      xposedEntry.hook(method3, PrivilegeManagerHooker.class);
      xposedEntry.hook(method4, PrivilegeManagerHooker.class);
      xposedEntry.hook(method5, PrivilegeManagerHooker.class);
      xposedEntry.hook(method6, PrivilegeManagerHooker.class);
      xposedEntry.hook(method7, PrivilegeManagerHooker.class);
      xposedEntry.hook(method8, PrivilegeManagerHooker.class);
      xposedEntry.hook(method9, PrivilegeManagerHooker.class);
      xposedEntry.hook(method10, PrivilegeManagerHooker.class);
      xposedEntry.hook(method11, PrivilegeManagerHooker.class);
      xposedEntry.hook(method12, PrivilegeManagerHooker.class);
      xposedEntry.hook(method13, PrivilegeManagerHooker.class);

    } catch (Throwable t) {
      xposedEntry.logE("[HookManager.hookPrivilegeManager] 挂钩权限管理器失败！", t);
    }
  }

  /** 挂钩Native库 作用：处理Native层的签名校验和VIP/广告逻辑 计划挂钩：libmetasec_ml.so（签名校验）、libdragoncore.so（VIP和广告控制） */
  public static void hookNativeLibrary() {
    // 待实现：通过Xposed的Native Hook能力挂钩指定so库中的方法
  }

  /**
   * 加载目标类的工具方法（封装类加载逻辑，减少重复代码）
   *
   * @param classLoader 目标类加载器
   * @param className 目标类全限定名
   * @return 加载后的类对象
   * @throws ClassNotFoundException 类未找到时抛出
   */
  private static Class<?> loadTargetClass(ClassLoader classLoader, String className)
      throws ClassNotFoundException {
    return classLoader.loadClass(className);
  }
}
