package com.xxhy.fqhelper.xposed.hooker;

import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.util.Arrays;
import java.util.Random;
import org.joor.Reflect;

/** VIP信息模型钩子处理器 用于Hook VIP信息模型（VipInfoModel）的构造方法，在本地VIP功能开启时篡改VIP初始化参数，模拟会员状态 */
@XposedHooker
public class VipInfoModelHooker implements XposedInterface.Hooker {
  // 随机标识值，用于钩子上下文区分（多线程环境下隔离不同调用）
  private int magicNumber;

  /**
   * 构造方法
   *
   * @param magic 随机生成的标识值
   */
  public VipInfoModelHooker(int magic) {
    this.magicNumber = magic;
  }

  /**
   * 构造方法调用前的钩子处理
   *
   * @param callback 钩子回调对象，包含构造方法参数等信息
   * @return 当前钩子实例，用于传递上下文到AfterInvocation 说明：当本地VIP功能开启且用户已登录时，修改VIP信息模型的初始化参数，模拟会员数据
   */
  @BeforeInvocation
  public static VipInfoModelHooker beforeInvocation(XposedInterface.BeforeHookCallback callback) {
    // 生成随机标识值，用于上下文区分
    int randomKey = new Random().nextInt();

    // 获取SP工具实例，读取本地VIP开关配置
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    if (sp.getBoolean(SPConstants.LOCAL_VIP, SPConstants.DEFAULT_LOCAL_VIP)) {
      // 检查用户是否登录（未登录则不修改参数）
      boolean isLogin =
          Reflect.onClass(
                  "com.dragon.read.user.AcctManager", // 账户管理类
                  DragonGlobals.getDragonClassLoader() // 应用类加载器
                  )
              .call("M") // 调用账户管理类的M方法获取用户信息对象
              .call("islogin") // 调用用户信息对象的islogin方法检查登录状态
              .get(); // 获取检查结果（boolean类型）

      if (!isLogin) {
        return new VipInfoModelHooker(randomKey);
      }

      // 获取构造方法的参数数组，准备篡改VIP初始化数据
      Object[] constructorArgs = callback.getArgs();

      // 篡改参数：设置VIP过期时间（未来时间戳）
      constructorArgs[0] = "218342534400";
      // 篡改参数：标记为VIP（1表示是VIP）
      constructorArgs[1] = "1";
      // 篡改参数：设置剩余VIP时间
      constructorArgs[2] = "218342534400";
      // 篡改参数：开启自动续费
      constructorArgs[3] = true;
      // 篡改参数：标记为联合VIP
      constructorArgs[4] = true;
      // 篡改参数：设置联合会员来源（1表示特定来源）
      constructorArgs[5] = 1;
      // 篡改参数：标记为广告VIP（免广告权限）
      constructorArgs[6] = true;
      // 篡改参数：设置VIP子类型为默认类型
      constructorArgs[7] =
          Reflect.onClass(
                  "com.dragon.read.rpc.model.VipCommonSubType", // VIP子类型枚举类
                  DragonGlobals.getDragonClassLoader())
              .field("Default") // 获取"Default"类型枚举值
              .get();

    }

    return new VipInfoModelHooker(randomKey);
  }

  /**
   * 构造方法调用后的钩子处理
   *
   * @param callback 钩子回调对象，包含构造方法返回的实例等信息
   * @param hookContext 钩子上下文实例（由beforeInvocation返回） 说明：当前无额外处理逻辑，仅作为钩子接口必填实现
   */
  @AfterInvocation
  public static void afterInvocation(
      XposedInterface.AfterHookCallback callback, VipInfoModelHooker hookContext) {
    // 无需额外处理，参数已在BeforeInvocation中修改完成
  }
}
