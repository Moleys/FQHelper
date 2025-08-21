package com.xxhy.fqhelper.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * JSON处理工具类 基于Gson库实现对象与JSON字符串的序列化/反序列化，支持格式化输出、泛型解析等功能
 *
 * <p>注意：依赖Gson库，需确保项目中已引入相关依赖
 */
public class JsonUtils {

  /** 默认Gson实例（非格式化输出，使用Gson默认配置） 默认行为：序列化null值、不格式化输出、使用默认日期格式等 */
  private static final Gson DEFAULT_GSON = new Gson();

  /** 格式化输出的Gson实例（带缩进的格式化JSON） 其他配置与DEFAULT_GSON一致 */
  private static final Gson PRETTY_GSON =
      new GsonBuilder()
          .setPrettyPrinting() // 启用格式化输出
          .create();

  /**
   * 将对象序列化为JSON字符串（非格式化）
   *
   * @param obj 待序列化的对象（可为null）
   * @return 序列化后的JSON字符串；若obj为null，返回"null"
   */
  public static String toJson(Object obj) {
    // 获取SP工具实例（上下文为Dragon应用，SP文件名从常量类获取）
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    // 从SP中读取调试模式开关状态（默认值从常量类获取）
    boolean debugMode = sp.getBoolean(SPConstants.DEBUG_MODE, SPConstants.DEFAULT_DEBUG_MODE);
    // 调试模式开启时，返回格式化JSON
    if (debugMode) {
      return toPrettyJson(obj);
    }
    return DEFAULT_GSON.toJson(obj);
  }

  /**
   * 将对象序列化为JSON并写入Writer（非格式化）
   *
   * @param src 待序列化的对象（可为null）
   * @param writer 接收JSON输出的Writer
   * @throws IOException 当写入Writer时发生I/O错误
   */
  public static void toJson(Object src, Writer writer) throws IOException {
    DEFAULT_GSON.toJson(src, writer);
  }

  /**
   * 将对象序列化为格式化的JSON字符串（带缩进） 适用于日志打印、调试等需要可读性的场景
   *
   * @param obj 待序列化的对象（可为null）
   * @return 格式化的JSON字符串；若obj为null，返回"null"
   */
  public static String toPrettyJson(Object obj) {
    return PRETTY_GSON.toJson(obj);
  }

  /**
   * 将JSON字符串反序列化为指定类型的对象
   *
   * @param json 待解析的JSON字符串（可为null或空字符串）
   * @param clazz 目标对象的Class类型
   * @param <T> 目标对象的类型
   * @return 反序列化后的对象；若json为null，返回null；若解析失败可能抛出异常
   */
  public static <T> T fromJson(String json, Class<T> clazz) {
    if (json == null) {
      return null;
    }
    return DEFAULT_GSON.fromJson(json, clazz);
  }

  /**
   * 将JSON字符串反序列化为泛型类型的对象（如List<String>、Map<Integer, User>等） 示例：解析List<String>可通过{@code
   * TypeToken.getParameterized(List.class, String.class).getType()}获取Type
   *
   * @param json 待解析的JSON字符串（可为null或空字符串）
   * @param type 目标泛型类型（通过{@link TypeToken}创建）
   * @param <T> 目标对象的类型
   * @return 反序列化后的泛型对象；若json为null，返回null；若解析失败可能抛出异常
   */
  public static <T> T fromJson(String json, Type type) {
    if (json == null) {
      return null;
    }
    return DEFAULT_GSON.fromJson(json, type);
  }

  /**
   * 从Reader中读取JSON并反序列化为指定类型的对象
   *
   * @param reader 提供JSON输入的Reader
   * @param clazz 目标对象的Class类型
   * @param <T> 目标对象的类型
   * @return 反序列化后的对象；若解析失败可能抛出异常
   * @throws IOException 当从Reader读取时发生I/O错误
   */
  public static <T> T fromJson(Reader reader, Class<T> clazz) throws IOException {
    return DEFAULT_GSON.fromJson(reader, clazz);
  }

  /**
   * 从Reader中读取JSON并反序列化为泛型类型的对象
   *
   * @param reader 提供JSON输入的Reader
   * @param type 目标泛型类型（通过{@link TypeToken}创建）
   * @param <T> 目标对象的类型
   * @return 反序列化后的泛型对象；若解析失败可能抛出异常
   * @throws IOException 当从Reader读取时发生I/O错误
   */
  public static <T> T fromJson(Reader reader, Type type) throws IOException {
    return DEFAULT_GSON.fromJson(reader, type);
  }
}
