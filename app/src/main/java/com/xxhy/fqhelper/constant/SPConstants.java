package com.xxhy.fqhelper.constant;

/**
 * SharedPreferences相关常量类
 * 存储SP文件名、各配置项的键名及默认值，统一管理本地配置常量
 */
public class SPConstants {
  public static final String SP_NAME = "fqhelper";

  public static final String PORT = "port";
  public static final String DEFAULT_PORT = "9999";

  public static final String START_WITH_APP = "start_with_app";
  public static final boolean DEFAULT_START_WITH_APP = false;

  public static final String START_WITH_SERVICE = "start_with_service";
  public static final boolean DEFAULT_START_WITH_SERVICE = false;

  public static final String BLOCK_UPDATES = "block_updates";
  public static final boolean DEFAULT_BLOCK_UPDATES = false;

  public static final String MAPPING = "mapping";
  public static final String DEFAULT_MAPPING = "{}";

  public static final String DEBUG_MODE = "debug_mode";
  public static final boolean DEFAULT_DEBUG_MODE = true;

  public static final String LOCAL_VIP = "local_vip";
  public static final boolean DEFAULT_LOCAL_VIP = false;
    
}
