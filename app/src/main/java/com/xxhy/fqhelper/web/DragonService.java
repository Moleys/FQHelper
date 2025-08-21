package com.xxhy.fqhelper.web;

import com.xxhy.fqhelper.utils.FieldNameUtils;
import com.xxhy.fqhelper.utils.JsonUtils;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import java.util.List;
import java.util.Map;
import org.joor.Reflect;

/**
 * Dragon服务类：提供与应用的RPC交互功能，封装了搜索书籍、获取书籍详情、音频播放地址、目录、内容等核心操作 内部通过反射调用目标应用的RPC接口，处理请求对象构建、参数设置及响应处理
 */
public class DragonService {

  // ============================= 常量定义 =============================
  // 类名常量（RPC请求模型类）
  private static final String GET_SEARCH_PAGE_REQUEST_CLASS =
      "com.dragon.read.rpc.model.GetSearchPageRequest";
  private static final String SEARCH_SOURCE_CLASS = "com.dragon.read.rpc.model.SearchSource";
  private static final String SEARCH_TAB_TYPE_CLASS = "com.dragon.read.rpc.model.SearchTabType";
  private static final String BOOK_DETAIL_REQUEST_CLASS =
      "com.dragon.read.rpc.model.BookDetailRequest";
  private static final String AUDIO_PLAY_URL_REQUEST_CLASS =
      "com.dragon.read.rpc.model.AudioPlayURLRequest";
  private static final String AUDIO_PLAY_URL_REQ_TYPE_CLASS =
      "com.dragon.read.rpc.model.AudioPlayUrlReqType";
  private static final String TONE_QUALITY_CLASS = "com.dragon.read.rpc.model.ToneQuality";
  private static final String GET_DIRECTORY_REQUEST_CLASS =
      "com.dragon.read.rpc.model.GetDirectoryForItemIdRequest";
  private static final String FULL_REQUEST_CLASS = "com.dragon.read.rpc.model.FullRequest";
  private static final String SAAS_ITEM_CONTENT_CLASS =
      "readersaas.com.dragon.read.saas.rpc.model.ItemContent";
  private static final String GET_BOOK_SHELF_INFO_REQUEST_CLASS =
      "com.dragon.read.rpc.model.GetBookShelfInfoRequest";
  private static final String GET_BOOK_MALL_REQUEST_CLASS =
      "com.dragon.read.rpc.model.GetBookMallCellChangeRequest";
  private static final String GET_NEW_CATEGORY_REQUEST_CLASS =
      "com.dragon.read.rpc.model.GetNewCategoryLandingPageRequest";
  private static final String REGISTER_KEY_REQUEST_CLASS =
      "readersaas.com.dragon.read.saas.rpc.model.RegisterKeyRequest";

  // 类/方法/字段名常量
  // 这一个类的方法名通常不变，就不写到这里了
  private static final String CLASS_R63_A = "r63.a";
  private static final String CLASS_R63_D = "r63.d";

  // 字段值常量
  private static final long TONE_ID = 80L; // 多角色对话升级版

  // ============================= 核心业务方法 =============================

  /**
   * 搜索书籍
   *
   * @param keyword 搜索关键词
   * @param page 页码（从1开始）
   * @return 搜索结果（Observable的阻塞结果）或异常对象
   */
  public static Object search(String keyword, int page) {
    try {
      // 创建搜索请求对象
      Object searchRequest = createRequestObject(GET_SEARCH_PAGE_REQUEST_CLASS);

      // 设置搜索请求参数
      Reflect.on(searchRequest)
          .set("bookshelfSearchPlan", 4) // 书架搜索计划
          .set("bookstoreTab", 2) // 书店标签
          .set("clickedContent", "page_search_button") // 点击来源
          .set("query", keyword); // 搜索关键词

      // 设置搜索源（枚举类型）
      Class<?> searchSourceClass =
          Class.forName(SEARCH_SOURCE_CLASS, true, DragonGlobals.getDragonClassLoader());
      Object searchSource = Reflect.onClass(searchSourceClass).call("findByValue", 1).get();
      Reflect.on(searchRequest).set("searchSource", searchSource);

      // 获取用户是否登录
      boolean isLogin =
          Reflect.onClass(
                  "com.dragon.read.user.AcctManager", // 账户管理类
                  DragonGlobals.getDragonClassLoader() // 应用类加载器
                  )
              .call("M") // 调用账户管理类的M方法获取用户信息对象
              .call("islogin") // 调用用户信息对象的islogin方法检查登录状态
              .get(); // 获取检查结果（boolean类型）
      short loginStatus = 0;
      if (isLogin) {
        loginStatus = 1;
      }

      int pageSize = 10; // 每页数据量
      // 设置其他搜索参数
      Reflect.on(searchRequest)
          .set("searchSourceId", "clks###")
          .set("tabName", "store")
          .set("tabType", getSearchTabType()) // 搜索标签类型（枚举）
          .set("userIsLogin", loginStatus) // 是否登录
          .set("offset", (page - 1) * pageSize) // 偏移量（分页）
          .set("passback", String.valueOf((page - 1) * pageSize)); // 新版中int转为String

      // 调用搜索接口
      return callFunction(CLASS_R63_A, searchRequest, "h0");
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.search] 搜索书籍失败，关键词：" + keyword + "，页码：" + page, t);
      return t;
    }
  }

  /**
   * 搜索书籍（默认第一页）
   *
   * @param keyword 搜索关键词
   * @return 搜索结果（Observable的阻塞结果）或异常对象
   */
  public static Object search(String keyword) {
    return search(keyword, 1);
  }

  /**
   * 获取书籍详情
   *
   * @param bookId 书籍ID（字符串形式的数字）
   * @return 书籍详情结果或异常对象
   */
  public static Object getDetail(String bookId) {
    // 参数验证
    if (!isValidNumericId(bookId)) {
      IllegalArgumentException e = new IllegalArgumentException("无效的bookId：" + bookId);
      LogUtils.logE("[DragonService.getDetail] 书籍ID无效：" + bookId, e);
      return e;
    }

    try {
      // 创建书籍详情请求对象并设置书籍ID
      Class<?> detailRequestClass =
          Class.forName(BOOK_DETAIL_REQUEST_CLASS, true, DragonGlobals.getDragonClassLoader());
      Object detailRequest = Reflect.onClass(detailRequestClass).create().get();
      Reflect.on(detailRequest).set("bookId", Long.parseLong(bookId));

      // 调用详情接口
      return callFunction(CLASS_R63_A, detailRequest, "h");
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.getDetail] 获取书籍详情失败，bookId：" + bookId, t);
      return t;
    }
  }

  /**
   * 获取音频播放地址
   *
   * @param bookId 书籍ID（字符串形式的数字）
   * @param itemId 章节/条目ID（字符串形式的数字）
   * @return 音频播放地址结果或异常对象
   */
  public static Object getAudioPlayURL(String bookId, String itemId) {
    // 参数验证
    if (!isValidNumericId(bookId) || !isValidNumericId(itemId)) {
      IllegalArgumentException e = new IllegalArgumentException("无效的bookId或itemId");
      LogUtils.logE(
          "[DragonService.getAudioPlayURL] 书籍ID或条目ID无效：bookId=" + bookId + ", itemId=" + itemId, e);
      return e;
    }

    try {
      // 创建音频播放请求对象
      Object audioRequest = createRequestObject(AUDIO_PLAY_URL_REQUEST_CLASS);

      // 设置音频请求参数
      Reflect.on(audioRequest)
          .set("bookId", Long.parseLong(bookId))
          .set("itemId", Long.parseLong(itemId))
          .set("toneId", TONE_ID) // 多角色对话版本
          .set("useServerHistory", false)
          .set("isToneInherit", true)
          .set("isLocalBook", false);

      // 设置请求类型（枚举）
      Object playReqType =
          Reflect.onClass(AUDIO_PLAY_URL_REQ_TYPE_CLASS, DragonGlobals.getDragonClassLoader())
              .field("PLAY")
              .get();
      Reflect.on(audioRequest).set("reqType", playReqType);

      // 设置音质（枚举）
      Object highQuality =
          Reflect.onClass(TONE_QUALITY_CLASS, DragonGlobals.getDragonClassLoader())
              .field("HighQuality")
              .get();
      Reflect.on(audioRequest).set("toneQuality", highQuality);

      // 调用音频地址接口
      return callFunction(CLASS_R63_A, audioRequest, "f");
    } catch (Throwable t) {
      LogUtils.logE(
          "[DragonService.getAudioPlayURL] 获取音频播放地址失败，bookId=" + bookId + ", itemId=" + itemId, t);
      return t;
    }
  }

  /**
   * 获取书籍目录
   *
   * @param bookId 书籍ID（字符串形式的数字）
   * @return 书籍目录结果或异常对象
   */
  public static Object getCatalog(String bookId) {
    // 参数验证
    if (!isValidNumericId(bookId)) {
      IllegalArgumentException e = new IllegalArgumentException("无效的bookId：" + bookId);
      LogUtils.logE("[DragonService.getCatalog] 书籍ID无效：" + bookId, e);
      return e;
    }

    try {
      // 创建目录请求对象并设置书籍ID
      Class<?> directoryRequestClass =
          Class.forName(GET_DIRECTORY_REQUEST_CLASS, true, DragonGlobals.getDragonClassLoader());
      Object directoryRequest = Reflect.onClass(directoryRequestClass).create().get();
      Reflect.on(directoryRequest).set("bookId", Long.parseLong(bookId));

      // 调用目录接口
      return callFunction(CLASS_R63_A, directoryRequest, "K");
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.getCatalog] 获取书籍目录失败，bookId：" + bookId, t);
      return t;
    }
  }

  /**
   * 获取书籍内容（包含解密逻辑）
   *
   * @param itemId 章节/条目ID
   * @return 解密后的书籍内容结果或异常对象
   */
  public static Object getContent(String itemId) {
    try {
      // 创建内容请求对象并设置条目ID
      Class<?> fullRequestClass =
          Class.forName(FULL_REQUEST_CLASS, true, DragonGlobals.getDragonClassLoader());
      Object fullRequest = Reflect.onClass(fullRequestClass).create().get();
      Reflect.on(fullRequest).set("itemId", itemId);

      // 调用内容接口获取原始数据
      Object itemObject = callFunction(CLASS_R63_D, fullRequest, "k");

      // 解密内容字段
      try {
        Object data = Reflect.on(itemObject).field("data").get();
        Reflect.on(itemObject).set("data", decodeContent(data)); // 替换为解密后的数据
      } catch (Throwable t) {
        LogUtils.logE("[DragonService.getContent] 解密书籍内容失败，itemId：" + itemId, t);
      }

      return itemObject;
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.getContent] 获取书籍内容失败，itemId：" + itemId, t);
      return t;
    }
  }

  /**
   * 获取书架信息
   *
   * @return 书架信息结果或异常对象
   */
  public static Object getBookShelfInfo() {
    try {
      // 创建书架请求对象
      Object shelfRequest = createRequestObject(GET_BOOK_SHELF_INFO_REQUEST_CLASS);
      // 调用书架接口
      return callFunction(CLASS_R63_A, shelfRequest, "z");
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.getBookShelfInfo] 获取书架信息失败", t);
      return t;
    }
  }

  /**
   * 获取书城信息
   *
   * @param parameters 请求参数映射（键为字段名，值为参数值）
   * @return 书城信息结果或异常对象
   */
  public static Object bookMall(Map<String, ?> parameters) {
    try {
      // 创建书城请求对象并设置参数
      Class<?> mallRequestClass =
          Class.forName(GET_BOOK_MALL_REQUEST_CLASS, true, DragonGlobals.getDragonClassLoader());
      Object mallRequest = Reflect.onClass(mallRequestClass).create().get();
      setRequestParameters(mallRequest, parameters);

      // 调用书城接口
      return callFunction(CLASS_R63_A, mallRequest, "j");
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.bookMall] 获取书城信息失败", t);
      return t;
    }
  }

  /**
   * 获取新分类信息
   *
   * @param parameters 请求参数映射（键为字段名，值为参数值）
   * @return 新分类信息结果或异常对象
   */
  public static Object newCategory(Map<String, ?> parameters) {
    try {
      // 创建分类请求对象并设置参数
      Class<?> categoryRequestClass =
          Class.forName(GET_NEW_CATEGORY_REQUEST_CLASS, true, DragonGlobals.getDragonClassLoader());
      Object categoryRequest = Reflect.onClass(categoryRequestClass).create().get();
      setRequestParameters(categoryRequest, parameters);

      // 调用分类接口
      return callFunction(CLASS_R63_A, categoryRequest, "U");
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.newCategory] 获取新分类信息失败", t);
      return t;
    }
  }

  // ============================= 辅助方法 =============================

  /**
   * 解密书籍内容 步骤：1. 获取原始加密内容 2. 转换为SAAS对象 3. 获取章节信息 4. 获取设备ID和用户ID 5. 注册密钥 6. 解密内容
   *
   * @param itemContent 原始内容对象（包含加密内容）
   * @return 解密后的内容对象或异常对象
   */
  private static Object decodeContent(Object itemContent) {
    try {
      // 获取原始加密内容
      String encryptedContent = Reflect.on(itemContent).field("content").get();

      // 转换为SAAS层内容对象
      Object saasItemContent = convertToSaasItemContent(itemContent);

      // 获取章节信息（包含bookId和chapterId）
      Object chapterInfo = getChapterInfo(saasItemContent);
      String bookId = Reflect.on(chapterInfo).field("bookId").get();
      String chapterId = Reflect.on(chapterInfo).field("chapterId").get();

      // 注册并获取解密密钥
      Object decryptKey = getDecryptKey(chapterInfo);
      LogUtils.logI("DecryptKey:" + JsonUtils.toJson(decryptKey));

      // 解密内容
      String decryptedContent = decryptContent(encryptedContent, decryptKey, bookId, chapterId);
      Reflect.on(itemContent).set("content", decryptedContent);

      return itemContent;
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.decodeContent] 解密内容失败", t);
      return t;
    }
  }

  /**
   * 调用目标类的静态方法并阻塞获取第一个结果
   *
   * @param className 目标类名
   * @param param 方法参数
   * @param methodName 方法名
   * @return 方法返回的结果（Observable.blockingFirst()）或异常对象
   */
  private static Object callFunction(String className, Object param, String methodName) {
    try {
      // 调用静态方法获取Observable
      Object observable =
          Reflect.onClass(className, DragonGlobals.getDragonClassLoader())
              .call(methodName, param)
              .get();
      // 阻塞获取第一个结果
      return Reflect.on(observable).call("blockingFirst").get();
    } catch (Throwable t) {
      LogUtils.logE(
          "[DragonService.callFunction] 调用目标方法失败，类：" + className + "，方法：" + methodName, t);
      return t;
    }
  }

  /**
   * 创建RPC请求对象
   *
   * @param className 请求类的全限定名
   * @return 创建的请求对象
   * @throws Throwable 创建过程中发生的异常
   */
  private static Object createRequestObject(String className) throws Throwable {
    return Reflect.onClass(className, DragonGlobals.getDragonClassLoader()).create().get();
  }

  /**
   * 为请求对象设置参数（处理列表参数取第一个值）
   *
   * @param request 请求对象
   * @param parameters 参数映射（键为字段名，值为参数值）
   */
  private static void setRequestParameters(Object request, Map<String, ?> parameters) {
    for (Map.Entry<String, ?> entry : parameters.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      // 若参数是列表，取第一个元素作为值
      if (value instanceof List) {
        List<?> list = (List<?>) value;
        if (!list.isEmpty()) {
          setField(request, key, list.get(0));
        }
      } else {
        setField(request, key, value);
      }
    }
  }

  /**
   * 根据字段类型自动转换并设置值
   *
   * @param obj 目标对象
   * @param fieldName 字段名（下划线形式，将转为驼峰）
   * @param value 要设置的值
   */
  private static void setField(Object obj, String fieldName, Object value) {
    try {
      // 下划线转驼峰（适配字段命名规范）
      String camelFieldName = FieldNameUtils.underlineToCamel(fieldName);
      // 获取字段当前值（用于判断类型）
      Object fieldValue = Reflect.on(obj).get(camelFieldName);
      String valueStr = value.toString();

      // 根据字段类型转换并设置值
      if (fieldValue instanceof Short) {
        Reflect.on(obj).set(camelFieldName, Short.parseShort(valueStr));
      } else if (fieldValue instanceof Integer) {
        Reflect.on(obj).set(camelFieldName, Integer.parseInt(valueStr));
      } else if (fieldValue instanceof Long) {
        Reflect.on(obj).set(camelFieldName, Long.parseLong(valueStr));
      } else if (fieldValue instanceof Float) {
        Reflect.on(obj).set(camelFieldName, Float.parseFloat(valueStr));
      } else if (fieldValue instanceof Boolean) {
        Reflect.on(obj).set(camelFieldName, Boolean.parseBoolean(valueStr));
      } else if (fieldValue.getClass().isEnum()) {
        // 枚举类型通过findByValue方法设置
        Class<?> enumClass = fieldValue.getClass();
        Object enumValue =
            Reflect.onClass(enumClass).call("findByValue", Integer.parseInt(valueStr)).get();
        Reflect.on(obj).set(camelFieldName, enumValue);
      } else {
        // 默认按字符串设置
        Reflect.on(obj).set(camelFieldName, valueStr);
      }
    } catch (Throwable t) {
      LogUtils.logE("[DragonService.setField] 设置字段失败，字段：" + fieldName + "，值：" + value, t);
    }
  }

  // ============================= 私有工具方法 =============================

  /** 获取搜索标签类型（枚举对象） */
  private static Object getSearchTabType() throws ClassNotFoundException {
    Class<?> tabTypeClass =
        Class.forName(SEARCH_TAB_TYPE_CLASS, true, DragonGlobals.getDragonClassLoader());
    return Reflect.onClass(tabTypeClass).call("findByValue", 1).get();
  }

  /**
   * 验证ID是否为有效的数字字符串
   *
   * @param id 待验证的ID
   * @return true-有效，false-无效
   */
  private static boolean isValidNumericId(String id) {
    if (id == null || id.isEmpty()) {
      return false;
    }
    try {
      Long.parseLong(id);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /** 将原始内容对象转换为SAAS层内容对象 */
  private static Object convertToSaasItemContent(Object itemContent) throws Throwable {
    Object target = createRequestObject(SAAS_ITEM_CONTENT_CLASS);
    return Reflect.onClass("com.dragon.read.util.m", DragonGlobals.getDragonClassLoader())
        .call("a", itemContent, target.getClass()) // 转换方法：(Object, Class) -> Object
        .get();
  }

  /** 获取章节信息对象 */
  private static Object getChapterInfo(Object saasItemContent) throws Throwable {
    return Reflect.onClass(
            "com.dragon.read.reader.download.ChapterInfo", DragonGlobals.getDragonClassLoader())
        .call("a", saasItemContent, false) // 转换方法：(SaasItemContent, boolean) -> ChapterInfo
        .get();
  }

  /**
   * 注册并获取用于章节内容解密的 DecryptKey。
   *
   * <p>整体流程： 1. 拿到 CryptManager 单例 2. 判断是否开启了“加密保护”开关 3. 提取当前用户 ID（可能）和章节 keyVersion 4.
   * 根据开关结果，优先使用同步接口（q / p）获取密钥 5. 如果同步接口返回 null，则降级到异步接口（s → blockingGet）
   *
   * @param chapterInfoObj 章节信息对象，必须包含 int 类型的 keyVersion 字段
   * @return 解密用的 DecryptKey 对象（实际类型可能是 Single<DecryptKey> 或 DecryptKey， 由调用方根据需要自行转换）
   * @throws Throwable 反射过程中任何一步出错都会原样抛出
   */
  private static Object getDecryptKey(Object chapterInfoObj) throws Throwable {
    // 获取 CryptManager 单例（内部静态字段 a）
    Object cryptManager =
        Reflect.onClass(
                "com.dragon.read.util.crypt.CryptManager", DragonGlobals.getDragonClassLoader())
            .field("a")
            .get();

    // 读取“是否开启加密保护”的开关
    boolean isCryptProtectEnabled =
        Reflect.onClass(
                "com.dragon.read.util.crypt.CryptManager", DragonGlobals.getDragonClassLoader())
            .call("c", cryptManager)
            .get();

    Object decryptKeyObj = null;

    // 取当前用户 ID，用于获取密钥
    String str =
        Reflect.onClass("com.dragon.read.reader.depend.q0", DragonGlobals.getDragonClassLoader())
            .field("b")
            .call("g")
            .get();

    // 获取章节对应的 keyVersion（章节加密密钥版本号）
    int keyver = Reflect.on(chapterInfoObj).field("keyVersion").get();

    // 根据开关调用不同同步接口
    // TODO:优化解密成功率，避免下载书籍时出现解密失败
    if (isCryptProtectEnabled) {
      decryptKeyObj =
          Reflect.on(cryptManager)
              .call("q", str, keyver) // 加密保护开启时的同步获取方法
              .get();
    } else {
      decryptKeyObj =
          Reflect.on(cryptManager)
              .call("p", str, keyver) // 加密保护关闭时的同步获取方法
              .get();
    }

    // 同步接口拿不到密钥时，降级到异步接口，并阻塞等待结果
    if (decryptKeyObj == null) {
      // 接口返回 Single<DecryptKey>，通过 blockingGet 同步取出实际 DecryptKey
      decryptKeyObj = Reflect.on(cryptManager).call("s", str, keyver).call("blockingGet").get();
    }

    return decryptKeyObj;
  }

  /** 解密书籍内容 */
  private static String decryptContent(
      String encryptedContent, Object decryptKeyObj, String bookId, String chapterId)
      throws Throwable {
    return Reflect.onClass("com.dragon.read.reader.utils.m", DragonGlobals.getDragonClassLoader())
        .call(
            "b",
            encryptedContent,
            decryptKeyObj,
            true,
            bookId,
            chapterId) // 解密方法：(content, key, flag, bookId, chapterId) -> String
        .get();
  }
}
