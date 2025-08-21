package com.xxhy.fqhelper.xposed.hooker;

import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.joor.Reflect;

/** 权限管理钩子处理器 用于Hook权限管理类的方法，模拟VIP权限（如免广告、会员特权等），当本地VIP功能开启时篡改权限判断结果 */
@XposedHooker
public class PrivilegeManagerHooker implements XposedInterface.Hooker {
  // 随机标识值，用于钩子上下文区分（多线程环境下隔离不同调用）
  private int magicNumber;

  /**
   * 构造方法
   *
   * @param magic 随机生成的标识值
   */
  public PrivilegeManagerHooker(int magic) {
    this.magicNumber = magic;
  }

  /**
   * 方法调用前的钩子处理
   *
   * @param callback 钩子回调对象，包含调用相关信息
   * @return 当前钩子实例，用于传递上下文到AfterInvocation 说明：生成随机标识并创建钩子实例，确保多线程场景下上下文正确关联
   */
  @BeforeInvocation
  public static PrivilegeManagerHooker beforeInvocation(
      XposedInterface.BeforeHookCallback callback) {
    int randomKey = new Random().nextInt();
    // 获取SP工具实例，读取本地VIP开关配置
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    if (sp.getBoolean(SPConstants.LOCAL_VIP, SPConstants.DEFAULT_LOCAL_VIP)) {
      // 检查用户是否登录（未登录则不处理权限模拟）
      boolean isLogin =
          Reflect.onClass(
                  "com.dragon.read.user.AcctManager", // 账户管理类
                  DragonGlobals.getDragonClassLoader() // 应用类加载器
                  )
              .call("M") // 调用账户管理类的M方法获取用户信息对象
              .call("islogin") // 调用用户信息对象的islogin方法检查登录状态
              .get(); // 获取检查结果（boolean类型）
      if (!isLogin) {
        return new PrivilegeManagerHooker(randomKey);
      }
      Method method = (Method) callback.getMember();
      if ("updateVipInfo".equals(method.getName())) {
        Object[] args = callback.getArgs();
        // 构建默认VIP子类型对象
        Object defaultSubType =
            Reflect.onClass(
                    "com.dragon.read.rpc.model.VipCommonSubType", // VIP子类型枚举类
                    DragonGlobals.getDragonClassLoader())
                .field("Default") // 获取"Default"类型的枚举值
                .get();

        // 创建VIP信息模型实例（模拟会员信息）
        Object vipInfoModel =
            Reflect.onClass(
                    "com.dragon.read.user.model.VipInfoModel", // VIP信息模型类
                    DragonGlobals.getDragonClassLoader())
                // 调用构造方法初始化（参数：过期时间、是否VIP、剩余时间等）
                .create("218342534400", "1", "218342534400", true, true, 1, true, defaultSubType)
                .get();
        args[0] = vipInfoModel;
      }
    }
    return new PrivilegeManagerHooker(randomKey);
  }

  /**
   * 方法调用后的钩子处理
   *
   * @param callback 钩子回调对象，包含方法返回结果等信息
   * @param hookContext 钩子上下文实例（由beforeInvocation返回） 说明：当本地VIP功能开启时，修改权限管理类方法的返回结果，模拟VIP权限
   */
  @AfterInvocation
  public static void afterInvocation(
      XposedInterface.AfterHookCallback callback, PrivilegeManagerHooker hookContext) {
    // 获取SP工具实例，读取本地VIP开关配置
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);
    if (sp.getBoolean(SPConstants.LOCAL_VIP, SPConstants.DEFAULT_LOCAL_VIP)) {
      // 检查用户是否登录（未登录则不处理权限模拟）
      boolean isLogin =
          Reflect.onClass(
                  "com.dragon.read.user.AcctManager", // 账户管理类
                  DragonGlobals.getDragonClassLoader() // 应用类加载器
                  )
              .call("M") // 调用账户管理类的M方法获取用户信息对象
              .call("islogin") // 调用用户信息对象的islogin方法检查登录状态
              .get(); // 获取检查结果（boolean类型）
      if (!isLogin) {
        return;
      }

      // 获取当前被Hook的方法
      Method method = (Method) callback.getMember();

      // 1. 特殊处理getInstance方法（权限管理类的单例获取方法）
      if ("getInstance".equals(method.getName())) {
        // 获取原方法返回的单例对象
        Object privilegeManagerInstance = callback.getResult();

        // 构建默认VIP子类型对象
        Object defaultSubType =
            Reflect.onClass(
                    "com.dragon.read.rpc.model.VipCommonSubType", // VIP子类型枚举类
                    DragonGlobals.getDragonClassLoader())
                .field("Default") // 获取"Default"类型的枚举值
                .get();

        // 创建VIP信息模型实例（模拟会员信息）
        Object vipInfoModel =
            Reflect.onClass(
                    "com.dragon.read.user.model.VipInfoModel", // VIP信息模型类
                    DragonGlobals.getDragonClassLoader())
                // 调用构造方法初始化（参数：过期时间、是否VIP、剩余时间等）
                .create("218342534400", "1", "218342534400", true, true, 1, true, defaultSubType)
                .get();

        // 设置VIP信息到权限管理单例对象（反射修改私有字段）
        Reflect.on(privilegeManagerInstance)
            .set("vipInfoModel", vipInfoModel); // 覆盖私有字段vipInfoModel

        // 构建多类型VIP信息列表（覆盖多种会员子类型）
        List<Object> vipInfoList = new ArrayList<>();
        String[] subTypeArr = new String[] {"ShortStory", "Publish", "AdFree", "Default"};
        for (String subTypeStr : subTypeArr) {
          // 为每种子类型创建对应的VIP信息
          Reflect.on(vipInfoModel)
              .set(
                  "subType",
                  Reflect.onClass(
                          "com.dragon.read.rpc.model.VipCommonSubType",
                          DragonGlobals.getDragonClassLoader())
                      .field(subTypeStr)
                      .get());
          vipInfoList.add(vipInfoModel);
        }

        // 将VIP列表设置到权限管理单例对象（反射修改私有列表字段j）
        Reflect.on(privilegeManagerInstance).set("j", vipInfoList);

        // 确保单例对象自身引用正确（反射修改私有静态字段l）
        Reflect.on(privilegeManagerInstance).set("l", privilegeManagerInstance);

        // 更新方法返回结果为修改后的单例对象
        callback.setResult(privilegeManagerInstance);
        return;
      } else if ("updateVipInfo".equals(method.getName())) {
        return;
      } else if ("showPayVipEntranceInChapterEnd".equals(method.getName())) {
        callback.setResult(false); // 不显示章末“成为会员无广告畅读”
        return;
      } else if ("isBookAdFree".equals(method.getName())) {
        callback.setResult(1);
        return;
      }

      // 2. 其他权限相关方法：统一返回true（表示拥有该权限）
      callback.setResult(true);
    }
  }
}
