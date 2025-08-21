package com.xxhy.fqhelper.xposed.hooker;

import android.widget.Toast;
import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.utils.ToastUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.util.Random;

/** Xposed钩子类：用于拦截应用的更新检查逻辑 通过Hook更新管理器的相关方法，根据配置决定是否屏蔽更新检查 */
@XposedHooker
public class UpdateManagerHooker implements XposedInterface.Hooker {

  /** 钩子上下文标识（预留字段） 可用于在before和after回调之间传递临时数据，当前未实际使用 */
  private int magicNumber;

  /**
   * 构造方法：初始化钩子上下文标识
   *
   * @param magic 标识值（通常为随机数，用于区分不同的钩子调用实例）
   */
  public UpdateManagerHooker(int magic) {
    this.magicNumber = magic;
  }

  /**
   * 方法调用前的钩子处理 检查屏蔽更新配置，若开启则拦截原方法执行，阻止更新检查
   *
   * @param callback Xposed提供的前置钩子回调，用于控制方法执行流程
   * @return 当前钩子实例，用于传递到afterInvocation回调
   */
  @BeforeInvocation
  public static UpdateManagerHooker beforeInvocation(XposedInterface.BeforeHookCallback callback) {
    // 生成随机键（用于标识本次钩子调用，预留扩展）
    int randomHookKey = new Random().nextInt();

    // 获取SP配置工具（依赖全局Application实例）
    SPUtils sp =
        SPUtils.getInstance(
            DragonGlobals.getDragonApplication(), // 获取全局应用上下文
            SPConstants.SP_NAME // 指定SP文件名
            );

    // 检查是否开启"屏蔽更新"配置
    if (sp.getBoolean(SPConstants.BLOCK_UPDATES, SPConstants.DEFAULT_BLOCK_UPDATES)) {
      // 此处原本有弹窗提示，因避免干扰用户已禁用
      // ToastUtils.show("检查更新已被拦截！", Toast.LENGTH_SHORT);
      LogUtils.logI("检查更新已被拦截！");
      // 拦截原方法执行：直接返回null并跳过原方法调用
      callback.returnAndSkip(null);
    }

    // 返回当前钩子实例，用于后续afterInvocation回调
    return new UpdateManagerHooker(randomHookKey);
  }

  /**
   * 方法调用后的钩子处理（当前无实际逻辑） 可用于在原方法执行后进行额外处理，如修改返回值、记录日志等
   *
   * @param callback Xposed提供的后置钩子回调，包含原方法执行信息
   * @param hookContext 前置回调传递的钩子实例（包含magicNumber等上下文）
   */
  @AfterInvocation
  public static void afterInvocation(
      XposedInterface.AfterHookCallback callback, UpdateManagerHooker hookContext) {
    // 预留：原方法执行后的处理逻辑
  }
}
