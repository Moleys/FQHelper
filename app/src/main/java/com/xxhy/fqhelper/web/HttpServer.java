package com.xxhy.fqhelper.web;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.JsonUtils;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于NanoHTTPD的HTTP服务器实现类
 * 用于处理客户端的HTTP请求，通过路由映射将不同URI请求分发到对应的处理器，并返回处理结果
 */
public class HttpServer extends NanoHTTPD {

    // 单例实例，使用volatile保证多线程可见性
    private static volatile HttpServer INSTANCE;
    // Gson实例，用于JSON序列化/反序列化（线程安全）
    private static final Gson GSON = new Gson();
    // 路由映射表：URI路径 -> 对应的处理器
    private static final Map<String, RouteHandler> ROUTE_MAP = createRouteMap();

    /**
     * 获取单例实例（线程安全）
     * 从SP中读取端口配置，默认使用SPConstants中定义的默认端口
     */
    public static HttpServer getInstance() {
        if (INSTANCE == null) {
            synchronized (HttpServer.class) {
                if (INSTANCE == null) {
                    // 从SP获取端口配置，若未配置则使用默认值
                    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
                    String portStr = sp.getString(SPConstants.PORT, SPConstants.DEFAULT_PORT);
                    int port = Integer.parseInt(portStr); // 解析端口号（默认值确保格式正确）
                    INSTANCE = new HttpServer(port);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 私有构造方法，初始化服务器端口
     * @param port 服务器监听端口
     */
    private HttpServer(int port) {
        super(port);
    }

    /**
     * 初始化路由映射表
     * 将URI路径与DragonController中的处理方法关联
     */
    private static Map<String, RouteHandler> createRouteMap() {
        Map<String, RouteHandler> map = new HashMap<>(16); // 初始容量16，减少扩容开销
        map.put("/search", DragonController::search);          // 搜索接口
        map.put("/detail", DragonController::detail);          // 详情接口
        map.put("/catalog", DragonController::catalog);        // 目录接口
        map.put("/content", DragonController::content);        // 内容接口
        map.put("/audio", DragonController::audioPlayURL);     // 音频播放地址接口
        map.put("/bookshelf", DragonController::bookshelf);    // 书架接口
        map.put("/booksource", DragonController::importBookSource); // 导入书源接口
        map.put("/reading/bookapi/bookmall/cell/change/v1/", DragonController::bookMall); // 书城接口
        map.put("/reading/bookapi/new_category/landing/v/", DragonController::newCategory); // 新分类接口
        return map;
    }

    /**
     * 处理HTTP请求的核心方法
     * 解析请求信息，分发到对应路由处理器，构建并返回响应
     * @param session HTTP会话对象，包含请求信息
     * @return 处理后的HTTP响应
     */
    @Override
    public Response serve(IHTTPSession session) {
        try {
            String uri = session.getUri();       // 请求URI
            Method method = session.getMethod(); // 请求方法（GET/POST等）
            Map<String, String> headers = session.getHeaders(); // 请求头

            // 处理GET和POST方法
            if (Method.GET.equals(method) || Method.POST.equals(method)) {
                // 确保Content-Type包含UTF-8编码，避免中文乱码
                headers.computeIfPresent("content-type", 
                    (k, v) -> v.contains("charset") ? v : v + "; charset=UTF-8");

                // 测试接口：返回Hello World
                if (uri.endsWith("/hello")) {
                    return createCorsResponse("text/plain", "Hello World!", headers);
                }

                // 获取请求参数（GET的query参数或POST的表单参数）
                Map<String, List<String>> params = session.getParameters();
                // 路由处理并获取返回数据
                Object returnData = handleRoute(uri, params);
                // 构建响应并返回
                return buildResponse(returnData, headers);
            }

            // 不支持的请求方法，返回404
            return create404Response();
        } catch (Throwable e) {
            // 捕获所有异常，避免服务器崩溃，记录错误日志
            LogUtils.logE("[HttpServer.serve] 处理HTTP请求失败:", e);
            return createErrorResponse(e);
        }
    }

    /**
     * 根据请求URI匹配对应的路由处理器
     * 匹配规则：URI以路由表中的路径结尾即视为匹配
     * @param uri 请求URI
     * @param params 请求参数
     * @return 处理器返回的结果数据（可能为null）
     */
    private Object handleRoute(String uri, Map<String, List<String>> params) {
        for (Map.Entry<String, RouteHandler> entry : ROUTE_MAP.entrySet()) {
            if (uri.endsWith(entry.getKey())) {
                // 匹配到路由，调用对应处理器
                return entry.getValue().handle(params);
            }
        }
        // 无匹配路由，返回null（后续会处理为404）
        return null;
    }

    /**
     * 根据返回数据类型构建HTTP响应
     * 支持Bitmap（图片）和普通数据（文本/JSON）
     * @param returnData 处理器返回的数据
     * @param headers 请求头（用于处理CORS）
     * @return 构建完成的HTTP响应
     * @throws IOException 处理Bitmap时可能抛出IO异常
     */
    private Response buildResponse(Object returnData, Map<String, String> headers) throws IOException {
        if (returnData == null) {
            return create404Response(); // 无数据返回404
        }

        Response response;
        if (returnData instanceof Bitmap) {
            // 处理图片类型响应
            response = createBitmapResponse((Bitmap) returnData);
        } else {
            // 处理文本类型响应
            String dataStr = String.valueOf(returnData);
            // 根据内容判断MIME类型（JSON或普通文本）
            String mimeType = isJsonValid(dataStr) 
                ? "application/json; charset=UTF-8" 
                : "text/plain; charset=UTF-8";
            response = newFixedLengthResponse(Response.Status.OK, mimeType, dataStr);
        }

        // 添加CORS头信息
        return addCorsHeaders(response, headers);
    }

    /**
     * 构建Bitmap类型的响应（返回图片）
     * 将Bitmap压缩为PNG格式，转换为输入流返回
     * @param bitmap 图片对象
     * @return 包含图片数据的HTTP响应
     * @throws IOException 压缩或流处理异常
     */
    private Response createBitmapResponse(Bitmap bitmap) throws IOException {
        // 自动关闭流资源（try-with-resources）
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 压缩Bitmap为PNG（质量100%）
            bitmap.compress(CompressFormat.PNG, 100, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray)) {
                // 返回图片流，指定长度避免Chunked编码问题
                return newFixedLengthResponse(
                    Response.Status.OK, 
                    "image/png", 
                    inputStream, 
                    byteArray.length
                );
            }
        }
    }

    /**
     * 构建404响应（资源未找到）
     * 返回HTML格式的404页面
     * @return 404响应
     */
    private Response create404Response() {
        final String html404 = "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "  <meta charset=\"utf-8\">\n"
            + "  <title>404 Not Found</title>\n"
            + "  <style>\n"
            + "    body{font-family:sans-serif;text-align:center;margin-top:15%}\n"
            + "    h1{font-size:48px;color:#555}\n"
            + "    p{font-size:20px;color:#777}\n"
            + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <h1>404</h1>\n"
            + "  <p>资源未找到</p>\n"
            + "</body>\n"
            + "</html>";

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", html404);
    }

    /**
     * 构建服务器内部错误响应（500）
     * 将异常信息序列化为JSON返回
     * @param t 触发错误的异常
     * @return 500响应
     */
    private Response createErrorResponse(Throwable t) {
        return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR, 
            "application/json", 
            JsonUtils.toJson(t)
        );
    }

    /**
     * 为响应添加CORS（跨域资源共享）头信息
     * 允许跨域请求，支持GET和POST方法
     * @param response 原始响应
     * @param headers 请求头（用于获取Origin）
     * @return 添加CORS头后的响应
     */
    private Response addCorsHeaders(Response response, Map<String, String> headers) {
        response.addHeader("Access-Control-Allow-Methods", "GET,POST");
        // 允许请求的Origin，若不存在则允许所有（*）
        String origin = headers.get("origin");
        response.addHeader("Access-Control-Allow-Origin", Objects.requireNonNullElse(origin, "*"));
        return response;
    }

    /**
     * 创建带CORS头的文本响应
     * @param mimeType 内容类型
     * @param content 响应内容
     * @param headers 请求头
     * @return 带CORS头的响应
     */
    private Response createCorsResponse(String mimeType, String content, Map<String, String> headers) {
        Response response = newFixedLengthResponse(Response.Status.OK, mimeType, content);
        return addCorsHeaders(response, headers);
    }

    /**
     * 验证字符串是否为有效的JSON格式
     * @param json 待验证的字符串
     * @return 若为有效JSON返回true，否则false
     */
    public static boolean isJsonValid(String json) {
        try {
            GSON.fromJson(json, Object.class); // 尝试解析为JSON对象
            return true;
        } catch (JsonSyntaxException e) {
            // 解析失败，不是有效JSON
            return false;
        }
    }

    /**
     * 路由处理器函数式接口
     * 定义路由处理方法的签名：接收请求参数，返回处理结果
     */
    @FunctionalInterface
    private interface RouteHandler {
        Object handle(Map<String, List<String>> params);
    }

    /**
     * 重启服务器（先停止再启动）
     * @throws IOException 服务器启动/停止异常
     */
    public void restart() throws IOException {
        if (isAlive()) {
            stop(); // 若服务器已启动，先停止
        }
        start(); // 启动服务器
    }
}
