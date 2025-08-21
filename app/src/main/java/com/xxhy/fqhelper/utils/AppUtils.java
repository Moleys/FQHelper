package com.xxhy.fqhelper.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import java.util.Map;
import java.util.Map.Entry;
import org.joor.Reflect;

/**
 * 应用相关工具类
 * 注意：此类通过反射调用Android系统内部API（如ActivityThread），这些API不属于公开稳定接口，
 * 可能因Android版本、厂商定制而存在兼容性问题，使用时需谨慎评估风险。
 */
public class AppUtils {

    /**
     * 通过反射创建应用上下文（Application Context）
     * 原理：通过ActivityThread获取应用绑定信息，再通过ContextImpl创建上下文
     *
     * @return 应用级上下文，若反射调用失败可能返回null
     */
    public static Context createAppContext() {
        try {
            // 获取当前ActivityThread实例（Android系统内部管理Activity的核心类）
            Object activityThread = Reflect.onClass("android.app.ActivityThread")
                    .call("currentActivityThread")
                    .get();

            // 获取应用绑定数据（存储应用相关信息）
            Object appBindData = Reflect.on(activityThread)
                    .field("mBoundApplication")
                    .get();

            // 获取应用信息对象（包含PackageInfo等信息）
            Object appInfo = Reflect.on(appBindData)
                    .field("info")
                    .get();

            // 通过ContextImpl创建应用上下文
            return Reflect.onClass("android.app.ContextImpl")
                    .call("createAppContext", activityThread, appInfo)
                    .get();
        } catch (Exception e) {
            // 捕获反射过程中可能出现的异常（类不存在、方法找不到等）
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过反射获取Application实例
     * 原理：通过ActivityThread的getApplication()方法获取
     *
     * @return 应用的Application实例，若反射失败可能返回null
     */
    public static Application getApplication() {
        try {
            // 链式调用：获取ActivityThread -> 调用getApplication()
            return Reflect.onClass("android.app.ActivityThread")
                    .call("currentActivityThread")
                    .call("getApplication")
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取当前处于前台的Activity（非暂停状态）
     * 原理：从ActivityThread的mActivities中筛选未暂停的Activity
     *
     * @return 当前活动的Activity，若未找到或反射失败返回null
     */
    public static Activity getCurrentActivity() {
        try {
            // 获取ActivityThread实例
            Object activityThread = Reflect.onClass("android.app.ActivityThread")
                    .call("currentActivityThread")
                    .get();

            // 获取mActivities字段（存储ActivityRecord的Map，key为Token，value为ActivityRecord）
            Object activityRecords = Reflect.on(activityThread)
                    .field("mActivities")
                    .get();

            // 转换为Map类型（ActivityRecord是系统内部类，存储Activity状态信息）
            Map<?, ?> activityRecordMap = (Map<?, ?>) activityRecords;

            // 遍历所有ActivityRecord，寻找未暂停的Activity
            for (Object activityRecord : activityRecordMap.values()) {
                // 获取ActivityRecord中的paused字段（判断Activity是否处于暂停状态）
                boolean isPaused = Reflect.on(activityRecord)
                        .field("paused")
                        .get();

                if (!isPaused) {
                    // 返回未暂停的Activity实例
                    return Reflect.on(activityRecord)
                            .field("activity")
                            .get();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据类名获取目标Activity实例
     * 原理：遍历ActivityThread中管理的所有Activity，匹配类名
     *
     * @param targetClassName 目标Activity的完整类名（如"com.example.MainActivity"）
     * @return 匹配的Activity实例，若未找到或反射失败返回null
     */
    public static Activity getTargetActivity(String targetClassName) {
        // 参数校验：目标类名为空直接返回
        if (targetClassName == null || targetClassName.trim().isEmpty()) {
            return null;
        }

        try {
            // 获取Application实例
            Application application = getApplication();
            if (application == null) {
                return null;
            }

            // 从Application获取mLoadedApk（存储应用加载信息）
            Object loadedApk = Reflect.on(application)
                    .field("mLoadedApk")
                    .get();

            // 从LoadedApk获取ActivityThread实例
            Object activityThread = Reflect.on(loadedApk)
                    .field("mActivityThread")
                    .get();

            // 获取所有ActivityRecord（同getCurrentActivity逻辑）
            Object activityRecords = Reflect.on(activityThread)
                    .field("mActivities")
                    .get();

            // 遍历ActivityRecordMap，匹配类名
            if (activityRecords instanceof Map) {
                Map<?, ?> activityRecordMap = (Map<?, ?>) activityRecords;
                for (Entry<?, ?> entry : activityRecordMap.entrySet()) {
                    Object activityRecord = entry.getValue();
                    // 获取ActivityRecord中的Activity实例
                    Object activity = Reflect.on(activityRecord)
                            .field("activity")
                            .get();

                    // 比较类名是否匹配
                    if (activity != null && targetClassName.equals(activity.getClass().getName())) {
                        return (Activity) activity;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
