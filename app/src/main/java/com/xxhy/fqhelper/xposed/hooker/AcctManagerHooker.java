package com.xxhy.fqhelper.xposed.hooker;

import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.joor.Reflect;

/**
 * 账户管理钩子处理器
 * 用于修改账户相关信息，主要实现本地VIP模拟功能，通过Hook账户管理类的方法篡改用户会员信息
 */
@XposedHooker
public class AcctManagerHooker implements XposedInterface.Hooker {
  // 随机生成的标识值，用于钩子上下文区分
  private int magicNumber;

  /**
   * 构造方法
   * @param magic 随机生成的标识值
   */
  public AcctManagerHooker(int magic) {
    this.magicNumber = magic;
  }

  /**
   * 方法调用前的钩子处理
   * @param callback 钩子回调对象，包含调用相关信息
   * @return 当前钩子实例，用于传递上下文到AfterInvocation
   * 说明：生成随机标识值并创建钩子实例，主要用于多线程环境下的上下文隔离
   */
  @BeforeInvocation
  public static AcctManagerHooker beforeInvocation(XposedInterface.BeforeHookCallback callback) {
    // 生成随机整数作为标识
    int randomKey = new Random().nextInt();
    return new AcctManagerHooker(randomKey);
  }

  /**
   * 方法调用后的钩子处理
   * @param callback 钩子回调对象，包含调用结果等信息
   * @param hookContext 钩子上下文实例（由beforeInvocation返回）
   * 说明：若本地VIP功能开启，修改方法返回的用户信息，模拟VIP会员状态
   */
  @AfterInvocation
  public static void afterInvocation(
      XposedInterface.AfterHookCallback callback, AcctManagerHooker hookContext) {
    // 获取SP工具实例，用于读取配置
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    // 检查本地VIP开关是否开启（默认关闭）
    if (sp.getBoolean(SPConstants.LOCAL_VIP, SPConstants.DEFAULT_LOCAL_VIP)) {
      // 获取原方法调用结果（通常为用户信息对象）
      Object result = callback.getResult();

      // 使用反射工具检查用户是否已登录
      boolean isLogin = Reflect.on(result).call("islogin").get();
      // 未登录状态则不处理
      if (!isLogin) {
        return;
      }

      // 1. 获取用户模型对象（存储用户详细信息）
      Object userModel = Reflect.on(result).field("userModel").get();

      // 2. 修改用户基本信息，模拟VIP身份
      Reflect.on(userModel)
          .set("userName", "XuxiaHaiyun") // 修改用户名
          .set("description", "用户信息拦截成功") // 修改用户描述
          .set("adVipAvailable", true) // 开启免广告VIP权限
          .set("freeAd", true) // 开启免广告功能
          .set("freeAdDay", 100000f) // 设置免广告天数
          .set("freeAdExpire", 218342534400L) // 设置免广告过期时间（未来时间）
          .set("freeAdLeft", 218342534400L); // 设置剩余免广告次数

      // 3. 构建VIP信息对象并修改其属性
      Object vipInfo = Reflect.onClass(
              "com.dragon.read.rpc.model.VipInfo", // VIP信息模型类
              DragonGlobals.getDragonClassLoader() // 应用类加载器
          )
          .create() // 创建实例
          .get();
      Reflect.on(vipInfo)
          .set("expireTime", "218342534400") // VIP过期时间（未来时间）
          .set("isVip", "1") // 标记为VIP
          .set("isAdVip", true) // 广告VIP
          .set("isUnionVip", true) // 联合VIP
          .set("leftTime", "218342534400") // 剩余VIP时间
          .set("autoRenew", true) // 自动续费
          // 设置续费类型为年付
          .set("renewType", Reflect.onClass(
                  "com.dragon.read.rpc.model.VipRenewType",
                  DragonGlobals.getDragonClassLoader()
              ).field("VipRenewYear").get())
          .set("continueMonth", true) // 连续包月
          .set("continueMonthBuy", true); // 已购买连续包月

      // 4. 设置VIP子类型（默认类型）
      Reflect.on(vipInfo)
          .set("subType", Reflect.onClass(
                  "com.dragon.read.rpc.model.VipCommonSubType",
                  DragonGlobals.getDragonClassLoader()
              ).field("Default").get())
          .set("unionSource", 1); // 联合会员来源

      // 5. 将构建好的VIP信息设置到用户模型
      Reflect.on(userModel).set("vipInfo", vipInfo);

      // 6. 构建多类型VIP信息列表（覆盖多种会员子类型）
      List<Object> vipInfoList = new ArrayList<>();
      // 需要覆盖的VIP子类型数组
      String[] subTypeArr = new String[] {"ShortStory", "Publish", "AdFree", "Default"};
      for (String subTypeStr : subTypeArr) {
        // 为每种子类型创建VIP信息并添加到列表
        Reflect.on(vipInfo)
            .set("subType", Reflect.onClass(
                    "com.dragon.read.rpc.model.VipCommonSubType",
                    DragonGlobals.getDragonClassLoader()
                ).field(subTypeStr).get());
        vipInfoList.add(vipInfo);
      }
      // 将VIP列表设置到用户模型
      Reflect.on(userModel).set("vipInfoList", vipInfoList);

      // 7. 设置其他VIP相关属性
      Reflect.on(userModel)
          .set("vipLastExpiredTime", "218342534400"); // 上次VIP过期时间

      // 8. 处理VIP资料展示信息
      Object vipProfileShow = Reflect.on(userModel).field("vipProfileShow").get();
      // 若对象为空则创建实例
      if (vipProfileShow == null) {
        vipProfileShow = Reflect.onClass(
                "com.dragon.read.rpc.model.VipProfileShow",
                DragonGlobals.getDragonClassLoader()
            )
            .create()
            .get();
      }
      // 设置VIP资料展示属性
      Reflect.on(vipProfileShow)
          .set("couponStrategy", "show") // 显示优惠券策略
          .set("useLynx", true); // 使用Lynx渲染

      // 将VIP资料展示信息设置到用户模型
      Reflect.on(userModel).set("vipProfileShow", vipProfileShow);

      // 9. 更新用户模型到结果对象，并设置为新的返回值
      Reflect.on(result).set("userModel", userModel);
      callback.setResult(result);
    }
  }
}
