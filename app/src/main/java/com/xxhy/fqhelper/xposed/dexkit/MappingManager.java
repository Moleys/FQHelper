package com.xxhy.fqhelper.xposed.dexkit;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.utils.ToastUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import android.text.TextUtils;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 映射关系管理类
 * 负责映射数据（如混淆类名、方法名与原始名称的对应关系）的持久化存储（SharedPreferences）、加载及查询
 * 提供静态方法用于初始化映射和查询映射值，确保全局映射数据的一致性
 */
public class MappingManager {

    /** SP工具类实例，用于操作SharedPreferences */
    private final SPUtils sp;
    /** 全局共享的映射缓存，存储解析后的键值对 */
    private static Map<String, String> mappingCache;
    /** Gson实例（静态复用，避免重复创建） */
    private static final Gson GSON = new Gson();
    /** Map类型的Gson解析令牌（静态复用） */
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    /**
     * 构造方法
     * @param context 上下文，用于初始化SPUtils
     */
    public MappingManager(Context context) {
        this.sp = SPUtils.getInstance(context, SPConstants.SP_NAME);
    }

    /**
     * 保存映射关系到SharedPreferences
     * @param map 待保存的映射键值对（键：通常为混淆名称，值：通常为原始名称）
     */
    public void saveMapping(Map<String, String> map) {
        if (map == null) {
            map = Collections.emptyMap();
        }
        // 将Map转为JSON字符串存储
        String json = GSON.toJson(map);
        sp.put(SPConstants.MAPPING, json);
    }

    /**
     * 从SharedPreferences加载映射关系
     * @return 解析后的映射键值对；若解析失败返回空Map
     */
    public Map<String, String> loadMapping() {
        try {
            // 从SP获取JSON字符串（默认空Map的JSON）
            String json = sp.getString(SPConstants.MAPPING, "{}");
            if (TextUtils.isEmpty(json)) {
                return new HashMap<>();
            }
            // 解析JSON为Map
            Map<String, String> map = GSON.fromJson(json, MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            // 解析失败（如JSON格式错误）返回空Map
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 初始化全局映射缓存
     * 逻辑：若SP中是默认值（未初始化），则触发解析APK生成映射并保存；否则直接从SP加载到缓存
     */
    public static void initMapping() {
        // 避免重复初始化
        if (mappingCache != null) {
            return;
        }

        // 获取应用上下文（确保不为空）
        Context appContext = DragonGlobals.getDragonApplication();
        if (appContext == null) {
            ToastUtils.show("初始化失败：应用上下文为空");
            mappingCache = new HashMap<>();
            return;
        }

        SPUtils sp = SPUtils.getInstance(appContext, SPConstants.SP_NAME);
        String storedMapping = sp.getString(SPConstants.MAPPING, SPConstants.DEFAULT_MAPPING);

        // 判断是否为默认值（未初始化）
        if (TextUtils.equals(storedMapping, SPConstants.DEFAULT_MAPPING)) {
            // 提示用户正在解析
            ToastUtils.show("正在查找混淆信息，请稍候...");
            // 解析APK生成映射（注意：此操作可能耗时，建议在后台线程执行）
            String apkPath = appContext.getApplicationInfo().sourceDir;
            Map<String, String> newMapping = MappingAnalyzer.analyzeMapping(apkPath);
            // 保存映射到SP
            MappingManager manager = new MappingManager(appContext);
            manager.saveMapping(newMapping);
            // 初始化缓存
            mappingCache = newMapping;
        } else {
            // 从SP加载已有映射到缓存
            MappingManager manager = new MappingManager(appContext);
            mappingCache = manager.loadMapping();
        }
    }

    /**
     * 查询映射值
     * @param key 映射键（如混淆类名/方法名）
     * @return 对应的映射值（如原始类名/方法名）；若未找到或键为null，返回null
     */
    public static String getMappingValue(String key) {
        // 键为null直接返回null
        if (key == null) {
            return null;
        }
        // 若缓存未初始化，触发初始化
        if (mappingCache == null) {
            initMapping();
        }
        // 从缓存查询（避免空指针）
        return mappingCache != null ? mappingCache.get(key) : null;
    }

    /**
     * 清除映射缓存（用于调试或重新初始化场景）
     */
    public static void clearCache() {
        mappingCache = null;
    }
}
