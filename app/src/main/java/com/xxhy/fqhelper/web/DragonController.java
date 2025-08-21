package com.xxhy.fqhelper.web;

import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.JsonUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import java.util.List;
import java.util.Map;

/**
 * Dragon控制器类
 * 处理与书籍相关的各类Web请求，包括搜索、详情查询、目录获取、内容加载等功能
 * 所有方法均为静态方法，通过调用DragonService完成业务逻辑处理
 */
public class DragonController {

    /**
     * 私有构造方法，禁止实例化（工具类设计）
     */
    private DragonController() {}

    /**
     * 从请求参数中获取指定key对应的第一个字符串值
     * @param parameters 请求参数集合（key为参数名，value为参数值列表）
     * @param key 要获取的参数名
     * @return 参数值（若参数不存在或为空集合则返回null）
     */
    private static String getParameter(Map<String, List<String>> parameters, String key) {
        if (parameters.containsKey(key) && !parameters.get(key).isEmpty()) {
            return parameters.get(key).get(0);
        }
        return null;
    }

    /**
     * 从请求参数中获取指定key对应的整数型值
     * @param parameters 请求参数集合
     * @param key 要获取的参数名
     * @param defaultValue 参数不存在或解析失败时的默认值
     * @return 解析后的整数（若参数无效则返回默认值）
     */
    private static int getIntParameter(Map<String, List<String>> parameters, String key, int defaultValue) {
        String paramValue = getParameter(parameters, key);
        if (paramValue != null) {
            try {
                return Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                // 解析失败时使用默认值
            }
        }
        return defaultValue;
    }

    /**
     * 检查参数是否为空，为空则返回异常信息
     * @param paramValue 参数值
     * @param paramName 参数名
     * @return 若为空则返回异常对象，否则返回null
     */
    private static Exception checkParamEmpty(String paramValue, String paramName) {
        if (paramValue == null || paramValue.isEmpty()) {
            return new Exception("参数 " + paramName + " 不能为空");
        }
        return null;
    }

    /**
     * 处理书籍搜索请求
     * @param parameters 请求参数，包含：
     *                   - query：搜索关键词（必填）
     *                   - page：页码（可选，默认1）
     * @return 搜索结果的JSON字符串；若参数无效则返回异常对象
     */
    public static Object search(Map<String, List<String>> parameters) {
        // 获取搜索关键词并验证
        String keyword = getParameter(parameters, "query");
        Exception paramError = checkParamEmpty(keyword, "query");
        if (paramError != null) {
            return paramError;
        }

        // 获取页码（默认第1页）
        int page = getIntParameter(parameters, "page", 1);

        // 调用服务层执行搜索并返回JSON结果
        return JsonUtils.toJson(DragonService.search(keyword, page));
    }

    /**
     * 处理书籍详情请求
     * @param parameters 请求参数，包含：
     *                   - book_id：书籍ID（必填）
     * @return 书籍详情的JSON字符串；若参数无效则返回异常对象
     */
    public static Object detail(Map<String, List<String>> parameters) {
        // 获取书籍ID并验证
        String bookId = getParameter(parameters, "book_id");
        Exception paramError = checkParamEmpty(bookId, "book_id");
        if (paramError != null) {
            return paramError;
        }

        // 获取书籍类型（默认普通书）
        /*String bookType = getParameter(parameters, "book_type");
        if (bookType == null || bookType.isEmpty()) {
            bookType = "0";
        }*/
        // - book_type：书籍类型（可选，默认0；0-普通书，1-有声书）
        
        // 调用服务层获取详情并返回JSON结果
        return JsonUtils.toJson(DragonService.getDetail(bookId));
    }

    /**
     * 处理书籍目录请求
     * @param parameters 请求参数，包含：
     *                   - book_id：书籍ID（必填）
     * @return 书籍目录的JSON字符串；若参数无效则返回异常对象
     */
    public static Object catalog(Map<String, List<String>> parameters) {
        String bookId = getParameter(parameters, "book_id");
        Exception paramError = checkParamEmpty(bookId, "book_id");
        if (paramError != null) {
            return paramError;
        }

        return JsonUtils.toJson(DragonService.getCatalog(bookId));
    }

    /**
     * 处理书籍内容请求
     * @param parameters 请求参数，包含：
     *                   - item_id：章节ID（必填）
     * @return 章节内容的JSON字符串；若参数无效则返回异常对象
     */
    public static Object content(Map<String, List<String>> parameters) {
        String itemId = getParameter(parameters, "item_id");
        Exception paramError = checkParamEmpty(itemId, "item_id");
        if (paramError != null) {
            return paramError;
        }

        Object content = DragonService.getContent(itemId);
        return JsonUtils.toJson(content);
    }

    /**
     * 处理有声书播放地址请求
     * @param parameters 请求参数，包含：
     *                   - book_id：书籍ID（必填）
     *                   - item_id：章节ID（必填）
     * @return 有声书播放地址的JSON字符串；若参数无效则返回异常对象
     */
    public static Object audioPlayURL(Map<String, List<String>> parameters) {
        // 验证书籍ID
        String bookId = getParameter(parameters, "book_id");
        Exception paramError = checkParamEmpty(bookId, "book_id");
        if (paramError != null) {
            return paramError;
        }

        // 验证章节ID
        String itemId = getParameter(parameters, "item_id");
        paramError = checkParamEmpty(itemId, "item_id");
        if (paramError != null) {
            return paramError;
        }

        return JsonUtils.toJson(DragonService.getAudioPlayURL(bookId, itemId));
    }

    /**
     * 处理书架信息请求
     * @param parameters 无实际参数（预留）
     * @return 书架信息的JSON字符串
     */
    public static Object bookshelf(Map<String, List<String>> parameters) {
        Object result = DragonService.getBookShelfInfo();
        return JsonUtils.toJson(result);
    }

    /**
     * 处理书城信息请求
     * @param parameters 书城请求参数（具体参数由服务层定义）
     * @return 书城信息的JSON字符串
     */
    public static Object bookMall(Map<String, List<String>> parameters) {
        return JsonUtils.toJson(DragonService.bookMall(parameters));
    }

    /**
     * 处理新分类信息请求
     * @param parameters 分类请求参数（具体参数由服务层定义）
     * @return 分类信息的JSON字符串
     */
    public static Object newCategory(Map<String, List<String>> parameters) {
        return JsonUtils.toJson(DragonService.newCategory(parameters));
    }

    /**
     * 处理书籍源导入配置请求
     * 生成包含当前服务端口的书籍源配置JSON（用于客户端导入）
     * @param parameters 无实际参数（预留）
     * @return 替换端口后的书籍源配置JSON
     */
    public static Object importBookSource(Map<String, List<String>> parameters) {
        // 书籍源配置模板（包含端口占位符）
        String json = "[\n  {\n    \"bookSourceComment\": \"// 感谢明月照大江大佬\",\n    \"bookSourceGroup\": \"🍅 番茄\",\n    \"bookSourceName\": \"🍅 FQ Helper\",\n    \"bookSourceType\": 0,\n    \"bookSourceUrl\": \"http://localhost:###port####Debug\",\n    \"customOrder\": 25,\n    \"enabled\": true,\n    \"enabledCookieJar\": true,\n    \"enabledExplore\": true,\n    \"exploreUrl\": \"我的书架::http://localhost:###port###/bookshelf\",\n    \"lastUpdateTime\": 1754735796354,\n    \"respondTime\": 180000,\n    \"ruleBookInfo\": {\n      \"author\": \"$.author\",\n      \"coverUrl\": \"$.thumbUrl\",\n      \"init\": \"data\",\n      \"intro\": \"&nbsp;&nbsp;\\n📕 原名：{{$.originalBookName}}\\n📖 别名：{{$.aliasName}}\\n🌟 评分：{{$.score}}\\n🔗 来源：{{$.source}}\\n🕒 开坑：{{$.createTime##T|\\\\+.*## }}\\n🏷️ 标签：{{$.tags}}\\n🎭 主角：{{$.role##\\\\[|\\\\\\\"|\\\\]}}\\n👥 在线：{{$.readCount}}人在读{{\\\"\\\\n\\\"+\\\"​\\\"}}\\n📄 简介：{{$.bookAbstract}}{{\\\"\\\\n\\\"+\\\"​\\\"}}\\n📚 内容：{{$.content}}{{\\\"\\\\n\\\"+\\\"​\\\"}}\\n📍 {{$.copyrightInfo##，.*##。}}\\n@js:result.replace(/.+：(人在读)?\\\\n/g,\\\"\\\")\",\n      \"kind\": \"$.category\",\n      \"lastChapter\": \"$.lastChapterTitle\",\n      \"name\": \"$.bookName\",\n      \"tocUrl\": \"/catalog?book_id={{$.bookId}}\",\n      \"wordCount\": \"$.wordNumber\"\n    },\n    \"ruleContent\": {\n      \"content\": \"$.data.content\\n<js>\\nresult.replace(/<[?!][^>]+>/g,\\\"\\\").replace(/<style>.*<\\\\/style>/gs, \\\"\\\").replace(/http:\\\\/\\\\/p[\\\\d-]*novel - sign.byteimg.com\\\\/novel - pic\\\\/([a - f0 - 9]{32})/, \'https://p6-novel.byteimg.com/origin/novel-pic/$1\');\\n</js>\"\n    },\n    \"ruleExplore\": {\n      \"author\": \"<js>java.get(\\\"author\\\")</js>\",\n      \"bookList\": \"$.data.bookShelfInfo.*\",\n      \"bookUrl\": \"<js>java.get(\\\"bookUrl\\\")</js>\",\n      \"coverUrl\": \"<js>java.get(\\\"thumbUrl\\\")</js>\",\n      \"intro\": \"<js>java.get(\\\"bookAbstract\\\")</js>\",\n      \"kind\": \"<js>java.get(\\\"category\\\")</js>\",\n      \"lastChapter\": \"<js>java.get(\\\"lastChapterTitle\\\")</js>\",\n      \"name\": \"$.bookId\\n<js>\\nlet url=\\\"http://localhost:###port###/detail?book_id=\\\"+result\\nlet data=JSON.parse(java.ajax(url)).data\\njava.put(\\\"bookUrl\\\",url)\\njava.put(\\\"author\\\",data.author)\\njava.put(\\\"lastChapterTitle\\\",data.lastChapterTitle)\\njava.put(\\\"category\\\",data.category)\\njava.put(\\\"wordNumber\\\",data.wordNumber)\\njava.put(\\\"bookAbstract\\\",data.bookAbstract)\\njava.put(\\\"thumbUrl\\\",data.thumbUrl)\\nresult=data.bookName\\n</js>\",\n      \"wordCount\": \"<js>java.get(\\\"wordNumber\\\")</js>\"\n    },\n    \"ruleSearch\": {\n      \"author\": \"$.bookData[0].author\",\n      \"bookList\": \"$.searchTabs[0].data\",\n      \"bookUrl\": \"/detail?book_id={{$.bookData[0].bookId}}\",\n      \"checkKeyWord\": \"我的\",\n      \"coverUrl\": \"$.bookData[0].thumbUrl\",\n      \"intro\": \"$.bookData[0].bookAbstract\",\n      \"kind\": \"$.bookData[0].category\",\n      \"lastChapter\": \"$.bookData[0].lastChapterTitle\",\n      \"name\": \"$.bookData[0].bookName\",\n      \"wordCount\": \"$.bookData[0].wordNumber\"\n    },\n    \"ruleToc\": {\n      \"chapterList\": \"$.data.itemDataList || $.data.catalogData\",\n      \"chapterName\": \"$.title || $.catalogTitle\",\n      \"chapterUrl\": \"/content?item_id={{$.itemId}}\",\n      \"isPay\": \"$.needUnlock\",\n      \"isVip\": \"$.showVipTag\",\n      \"preUpdateJs\": \"java.refreshTocUrl();\"\n    },\n    \"searchUrl\": \"/search?query={{key}}&page={{page}}\",\n    \"weight\": 0\n  }\n]";
        
        // 从配置中获取服务端口，替换模板中的占位符
        SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
        String portStr = sp.getString(SPConstants.PORT, SPConstants.DEFAULT_PORT);
        return json.replace("###port###", portStr);
    }
}
