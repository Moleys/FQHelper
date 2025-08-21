package com.xxhy.fqhelper.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;

/**
 * Toast工具类
 * 提供在主线程显示Toast提示的功能，封装了线程切换逻辑，确保Toast在UI线程展示
 */
public class ToastUtils {

  /**
   * 显示长时长的Toast提示
   * @param text 要显示的提示文本内容
   * 说明：通过Handler将Toast显示操作切换到主线程（MainLooper）执行，避免在子线程中调用导致异常
   */
  public static void show(String text) {
    new Handler(Looper.getMainLooper())
        .post(
            () ->
                // 使用应用全局上下文创建Toast，显示时长为LENGTH_LONG（较长时间）
                Toast.makeText(DragonGlobals.getDragonApplication(), text, Toast.LENGTH_LONG)
                    .show());
  }

  /**
   * 显示指定时长的Toast提示
   * @param text 要显示的提示文本内容
   * @param duration 显示时长，可传入Toast.LENGTH_SHORT（短时间）或Toast.LENGTH_LONG（较长时间）
   * 说明：通过Handler切换到主线程执行，支持自定义显示时长
   */
  public static void show(String text, int duration) {
    new Handler(Looper.getMainLooper())
        .post(() -> 
            // 使用应用全局上下文创建Toast，显示时长由参数i指定
            Toast.makeText(DragonGlobals.getDragonApplication(), text, duration).show());
  }
    
}
