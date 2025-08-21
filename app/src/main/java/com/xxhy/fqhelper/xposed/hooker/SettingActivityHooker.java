package com.xxhy.fqhelper.xposed.hooker;

import android.content.Context;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import java.util.LinkedList;
import java.util.Random;
import org.joor.Reflect;

/**
 * 设置页面钩子处理器
 * 用于Hook设置页面（SettingsActivity）的列表构建方法，在设置项列表中插入自定义的"FQ Helper"功能项
 */
@XposedHooker
public class SettingActivityHooker implements XposedInterface.Hooker {
    // 随机标识值，用于钩子上下文区分（多线程环境下隔离不同调用）
    private int magicNumber;

    /**
     * 构造方法
     * @param magic 随机生成的标识值
     */
    public SettingActivityHooker(int magic) {
        this.magicNumber = magic;
    }

    /**
     * 方法调用前的钩子处理
     * @param callback 钩子回调对象，包含调用相关信息
     * @return 当前钩子实例，用于传递上下文到AfterInvocation
     * 说明：生成随机标识并创建钩子实例，为后置处理提供上下文
     */
    @BeforeInvocation
    public static SettingActivityHooker beforeInvocation(
            XposedInterface.BeforeHookCallback callback) {
        int randomKey = new Random().nextInt();
        return new SettingActivityHooker(randomKey);
    }

    /**
     * 方法调用后的钩子处理
     * @param callback 钩子回调对象，包含方法返回的设置项列表
     * @param hookContext 钩子上下文实例（由beforeInvocation返回）
     * 说明：在设置项列表构建完成后，插入自定义的"FQ Helper"项（确保仅插入一次）
     */
    @AfterInvocation
    public static void afterInvocation(
            XposedInterface.AfterHookCallback callback, SettingActivityHooker hookContext) {
        // 获取原方法返回的设置项列表（LinkedList类型）
        LinkedList settingItems = (LinkedList) callback.getResult();
        // 列表为空则直接返回，不做处理
        if (settingItems == null || settingItems.isEmpty()) {
            return;
        }

        // 检查列表中是否已存在"FQ Helper"项（避免重复插入）
        Object firstItem = settingItems.get(0);
        // 反射获取第一项的名称字段（假设字段"f"存储项名称），在类中的排序通常不变，但字段名可能变
        String firstItemName = Reflect.on(firstItem).field("f").get();
        // 若已存在则直接返回
        if ("FQ Helper".equals(firstItemName)) {
            return;
        }

        // 构建自定义的"FQ Helper"设置项
        // 获取当前设置页面实例（SettingsActivity）作为上下文
        Object settingActivity = callback.getThisObject();
        // 反射获取页面中的RecyclerView适配器对象（假设字段"a"存储适配器）
        Object recyclerViewAdapter = Reflect.on(settingActivity).field("a").get();

        // 创建自定义设置项实例（使用目标应用的设置项类）
        Object fqHelperItem = Reflect.onClass(
                "com.dragon.read.component.biz.impl.mine.settings.item.d", // 目标应用的设置项类
                DragonGlobals.getDragonClassLoader())
            // 调用构造方法初始化（参数：上下文、适配器）
            .create((Context) settingActivity, recyclerViewAdapter)
            .get();

        // 设置自定义项的名称为"FQ Helper"（假设字段"f"用于存储名称）
        Reflect.on(fqHelperItem).set("f", "FQ Helper");

        // 将自定义项插入到列表首位
        settingItems.add(0, fqHelperItem);
        // 更新方法返回结果为修改后的列表
        callback.setResult(settingItems);
    }
}
