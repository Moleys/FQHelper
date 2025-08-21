package com.xxhy.fqhelper.utils;

import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import com.xxhy.fqhelper.xposed.global.XposedGlobals;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 日志工具类
 * 提供日志打印、异常堆栈信息获取、日志写入文件等功能，且日志输出受调试模式控制
 */
public class LogUtils {

  /**
   * 打印普通信息日志
   * @param message 日志内容
   * 说明：仅当SP中配置的调试模式开启时，才通过Xposed框架打印日志
   */
  public static void logI(String message) {
    // 获取SP工具实例（上下文为Dragon应用，SP文件名从常量类获取）
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    // 从SP中读取调试模式开关状态（默认值从常量类获取）
    boolean debugMode = sp.getBoolean(SPConstants.DEBUG_MODE, SPConstants.DEFAULT_DEBUG_MODE);
    // 调试模式开启时，调用Xposed全局类的日志方法
    if (debugMode) {
      XposedGlobals.getXposedEntryInstance().log(message);
    }
  }

  /**
   * 打印错误信息日志
   * @param message 错误描述信息
   * @param throwable 异常对象
   * 说明：仅当调试模式开启时，通过Xposed框架打印错误日志及异常信息
   */
  public static void logE(String message, Throwable throwable) {
    // 获取SP工具实例，读取调试模式状态
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    boolean debugMode = sp.getBoolean(SPConstants.DEBUG_MODE, SPConstants.DEFAULT_DEBUG_MODE);
    // 调试模式开启时，调用Xposed全局类的错误日志方法
    if (debugMode) {
      XposedGlobals.getXposedEntryInstance().logE(message, throwable);
    }
  }

  /**
   * 获取异常的完整堆栈信息
   * @param t 异常对象
   * @return 包含完整堆栈信息的字符串
   */
  public static String getFullStackTrace(Throwable t) {
    // 通过StringWriter捕获异常堆栈信息
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    // 将异常堆栈信息写入PrintWriter
    t.printStackTrace(printWriter);
    // 关闭PrintWriter
    printWriter.close();
    // 返回捕获到的堆栈信息字符串
    return stringWriter.toString();
  }

  /**
   * 将日志内容追加写入到文件
   * @param text 要写入的日志内容
   * 说明：日志文件路径为应用数据目录下的"fqhelper.log"，采用同步方式写入避免并发问题
   */
  public static void appendLogToFile(String text) {
    // 获取SP工具实例，读取调试模式状态
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    boolean debugMode = sp.getBoolean(SPConstants.DEBUG_MODE, SPConstants.DEFAULT_DEBUG_MODE);
    // 非调试模式时，结束方法，避免 IO 操作影响性能
    if (!debugMode) return;

    // 创建日志文件对象（路径：应用数据目录 + "fqhelper.log"）
    File logFile = new File(DragonGlobals.getDragonApplication().getDataDir(), "fqhelper.log");
    // 若文件对象为空则直接返回
    if (logFile == null) {
      return;
    }

    // 日志内容末尾添加换行符（便于阅读）
    String line = text + "\n\n";

    // 同步代码块：保证多线程环境下日志写入的线程安全
    synchronized (LogUtils.class) {
      FileWriter fw = null;
      try {
        // 创建FileWriter（第二个参数为true表示追加模式）
        fw = new FileWriter(logFile, true);
        // 写入日志内容
        fw.write(line);
        // 刷新缓冲区
        fw.flush();
      } catch (IOException e) {
        // 写入失败时，调用错误日志方法记录异常
        logE("写日志失败", e);
      } finally {
        // 确保FileWriter关闭
        if (fw != null) {
          try {
            fw.close();
          } catch (IOException ignore) {
            // 关闭异常忽略处理
          }
        }
      }
    }
  }
}
