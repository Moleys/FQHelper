package com.xxhy.fqhelper.xposed.hooker;

import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.utils.NetworkUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.utils.ToastUtils;
import com.xxhy.fqhelper.web.HttpServer;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.io.IOException;
import java.util.Random;

/**
 * 下载通知服务钩子处理器
 * 用于Hook下载通知服务（DownloadNotificationService）的生命周期方法，在服务启动后根据配置启动或重启HTTP服务
 */
@XposedHooker
public class DragonServiceHooker implements XposedInterface.Hooker {
    // 随机标识值，用于钩子上下文区分（多线程环境下隔离不同调用）
    private int magicNumber;

    /**
     * 构造方法
     * @param magic 随机生成的标识值
     */
    public DragonServiceHooker(int magic) {
        this.magicNumber = magic;
    }

    /**
     * 方法调用前的钩子处理
     * @param callback 钩子回调对象，包含调用相关信息
     * @return 当前钩子实例，用于传递上下文到AfterInvocation
     * 说明：生成随机标识并创建钩子实例，为后续的后置处理提供上下文
     */
    @BeforeInvocation
    public static DragonServiceHooker beforeInvocation(XposedInterface.BeforeHookCallback callback) {
        int randomKey = new Random().nextInt();
        return new DragonServiceHooker(randomKey);
    }

    /**
     * 方法调用后的钩子处理
     * @param callback 钩子回调对象，包含方法返回结果等信息
     * @param hookContext 钩子上下文实例（由beforeInvocation返回）
     * 说明：在下载通知服务初始化完成后，根据配置重启HTTP服务并提示状态
     */
    @AfterInvocation
    public static void afterInvocation(
            XposedInterface.AfterHookCallback callback, DragonServiceHooker hookContext) {

        // 获取SP工具实例，读取"随服务启动"配置
        SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
        if (sp.getBoolean(SPConstants.START_WITH_SERVICE, SPConstants.DEFAULT_START_WITH_SERVICE)) {
            try {
                // 获取HTTP服务单例并重启（确保服务状态正确）
                HttpServer httpServer = HttpServer.getInstance();
                httpServer.restart();

                // 显示服务启动成功提示，包含本地IP和端口
                String ipAddress = NetworkUtils.getIPAddress(true); // 获取本地IPv4地址
                String port = sp.getString(SPConstants.PORT, SPConstants.DEFAULT_PORT); // 从配置获取端口
                LogUtils.logI("HTTP Server已启动\n" + ipAddress + ":" + port);
                ToastUtils.show("HTTP Server已启动\n" + ipAddress + ":" + port);

            } catch (IOException e) {
                // 记录服务启动失败日志
                LogUtils.logE("Failed to start HTTP server:", e);
                // 显示启动失败提示
                ToastUtils.show(e.toString());
            }
        }
    }
}
