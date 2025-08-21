package com.xxhy.fqhelper.xposed.hooker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.xxhy.fqhelper.constant.SPConstants;
import com.xxhy.fqhelper.utils.AppUtils;
import com.xxhy.fqhelper.utils.LogUtils;
import com.xxhy.fqhelper.utils.SPUtils;
import com.xxhy.fqhelper.utils.ToastUtils;
import com.xxhy.fqhelper.web.HttpServer;
import com.xxhy.fqhelper.xposed.global.DragonGlobals;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

import org.joor.Reflect;

/** Xposed钩子类 - 用于拦截"FQ Helper"设置项的点击事件，显示模块配置对话框 主要功能：通过Hook目标应用的设置项，注入自定义点击事件，展示配置界面供用户修改模块参数 */
@XposedHooker
public class SettingItemHooker implements XposedInterface.Hooker {

  /**
   * 钩子前置处理方法 拦截设置项的点击事件，当匹配到"FQ Helper"项时，替换为自定义点击事件（显示配置对话框）
   *
   * @param callback Xposed钩子回调对象，包含调用参数和上下文信息
   * @return 当前钩子实例
   */
  @BeforeInvocation
  public static SettingItemHooker beforeInvocation(XposedInterface.BeforeHookCallback callback) {
    // 获取设置项对象（从方法参数中）
    Object itemObject = callback.getArgs()[1];
    // 反射获取设置项名称（字段"e"存储名称），在类中的顺序大概不变
    String itemName = Reflect.on(itemObject).field("f").get();

    // 匹配目标设置项"FQ Helper"
    if ("FQ Helper".equals(itemName)) {
      // 获取当前视图对象（设置项的View）
      View view = (View) callback.getArgs()[0];
      // 获取上下文环境
      Context context = view.getContext();
      // 替换点击事件为显示配置对话框
      view.setOnClickListener(v -> showModuleConfigDialog(context, itemObject));
      // 跳过原方法执行
      callback.returnAndSkip(null);
    }
    return new SettingItemHooker();
  }

  /** 钩子后置处理方法（本场景无需处理，留空） */
  @AfterInvocation
  public static void afterInvocation(
      XposedInterface.AfterHookCallback callback, SettingItemHooker hookContext) {
    // 无后置处理逻辑
  }

  /**
   * 显示模块配置对话框 构建包含各种配置项（端口设置、开关选项等）的对话框，供用户修改模块参数
   *
   * @param context 上下文环境
   * @param itemObject 设置项对象（预留，暂未使用）
   */
  private static void showModuleConfigDialog(Context context, Object itemObject) {
    // 获取SP存储工具（用于读写配置参数）
    SPUtils sp = SPUtils.getInstance(DragonGlobals.getDragonApplication(), SPConstants.SP_NAME);

    // 1. 创建主容器布局（垂直排列所有控件）
    LinearLayout mainContainer = new LinearLayout(context);
    LinearLayout.LayoutParams mainContainerParams =
        getLinearLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(context, 20) // 内边距：上下左右20dp
            );
    mainContainer.setLayoutParams(mainContainerParams);
    mainContainer.setOrientation(LinearLayout.VERTICAL);
    mainContainer.setPadding(
        dpToPx(context, 20), dpToPx(context, 20), dpToPx(context, 20), dpToPx(context, 20));

    // 2. 添加HTTP服务器配置区域
    // 区域标题
    mainContainer.addView(createSectionTitle(context, "HTTP Server"));

    // 端口输入框
    EditText portEditText = createPortEditText(context, sp);
    mainContainer.addView(portEditText);

    // 随应用启动开关
    mainContainer.addView(
        createSwitch(
            context,
            "随应用启动",
            sp.getBoolean(SPConstants.START_WITH_APP, SPConstants.DEFAULT_START_WITH_APP),
            dpToPx(context, 12)));

    // 随服务启动开关
    mainContainer.addView(
        createSwitch(
            context,
            "随服务启动",
            sp.getBoolean(SPConstants.START_WITH_SERVICE, SPConstants.DEFAULT_START_WITH_SERVICE),
            dpToPx(context, 12)));

    // 忽略电池优化开关（带点击事件）
    mainContainer.addView(createBatteryOptSwitch(context, sp));

    // 3. 添加附加功能配置区域
    // 区域标题
    mainContainer.addView(createSectionTitle(context, "附加功能"));

    // 阻止更新开关
    mainContainer.addView(
        createSwitch(
            context,
            "阻止更新",
            sp.getBoolean(SPConstants.BLOCK_UPDATES, SPConstants.DEFAULT_BLOCK_UPDATES),
            dpToPx(context, 12)));

    // 本地会员开关
    mainContainer.addView(
        createSwitch(
            context,
            "本地会员",
            sp.getBoolean(SPConstants.LOCAL_VIP, SPConstants.DEFAULT_LOCAL_VIP),
            dpToPx(context, 12)));

    // 调试模式开关
    Switch debugModeSwitch =
        createSwitch(
            context,
            "调试模块",
            sp.getBoolean(SPConstants.DEBUG_MODE, SPConstants.DEFAULT_DEBUG_MODE),
            dpToPx(context, 12));
    // debugModeSwitch.setVisibility(View.GONE);
    // 隐藏调试模式开关
    mainContainer.addView(debugModeSwitch);

    // 4. 构建对话框
    AlertDialog configDialog =
        buildConfigDialog(context, mainContainer, sp, portEditText);

    // 5. 显示对话框并设置样式
    Window window = configDialog.getWindow();
    if (window != null) {
      WindowManager.LayoutParams params = window.getAttributes();
      // 设置对话框位置（底部居中）
      params.gravity = Gravity.BOTTOM | Gravity.CENTER;
      // 设置对话框宽度（屏幕宽度的90%）
      params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9);
      window.setAttributes(params);
      // 设置软键盘模式（避免遮挡输入框）
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    configDialog.show();

    // 显示对话框前设置模糊效果
    // Window window = configDialog.getWindow();

    // 仅在Android 12及以上版本设置模糊
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      try {

        // 设置"后方模糊"（模糊窗口背后的内容）
        WindowManager.LayoutParams params = window.getAttributes();
        // 添加模糊标记
        params.flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        // 设置模糊半径
        params.setBlurBehindRadius(dpToPx(context, 18)); // 模糊半径
        window.setAttributes(params);

        float radius = dpToPx(context, 20); // 转成 px
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(190, 255, 255, 255)); // 半透明白色
        bg.setCornerRadius(radius); // 圆角半径

        window.setBackgroundDrawable(bg); // 应用背景

        // 模糊当前窗口背景（磨砂玻璃效果）
        window.setBackgroundBlurRadius(dpToPx(context, 18)); // 模糊半径

      } catch (Exception e) {
        // 部分设备可能不支持模糊，捕获异常避免崩溃
        LogUtils.logE("设备不支持模糊效果: ", e);
      }
    }
  }

  /**
   * 创建区域标题TextView 用于分隔不同配置区域（如"HTTP Server"、"附加功能"）
   *
   * @param context 上下文
   * @param title 标题文字
   * @return 配置好的TextView
   */
  private static TextView createSectionTitle(Context context, String title) {
    TextView titleView = new TextView(context);
    LinearLayout.LayoutParams params =
        getLinearLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(context, 16) // 底部间距16dp
            );
    titleView.setLayoutParams(params);
    titleView.setText(title);
    titleView.setTextColor(Color.parseColor("#666666")); // 深灰色标题
    titleView.setTextSize(14);
    titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD); // 加粗
    return titleView;
  }

  /**
   * 创建端口输入框 用于用户输入HTTP服务器端口号，带提示和样式优化
   *
   * @param context 上下文
   * @param sp SP存储工具
   * @return 配置好的EditText
   */
  private static EditText createPortEditText(Context context, SPUtils sp) {
    EditText portEditText = new EditText(context);

    LinearLayout.LayoutParams params =
        getLinearLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(context, 20));
    portEditText.setLayoutParams(params);

    // 文本/提示
    portEditText.setHint("请输入端口号（0-65535）");
    portEditText.setHintTextColor(Color.parseColor("#CCCCCC"));
    portEditText.setText(sp.getString(SPConstants.PORT, SPConstants.DEFAULT_PORT));
    portEditText.setTextColor(Color.BLACK);
    portEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);

    // 半透明圆角背景（动态生成）
    float radius = dpToPx(context, 10);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.argb(51, 255, 255, 255)); // 20% 透明度的白色
    bg.setCornerRadius(radius);
    bg.setStroke(1, Color.argb(102, 200, 200, 200)); // 可选：1 px 淡灰色描边
    portEditText.setBackground(bg);

    // 内边距
    int padding = dpToPx(context, 12);
    portEditText.setPadding(padding, padding, padding, padding);

    return portEditText;
  }

  /**
   * 创建通用开关控件 封装开关的基本样式和属性，用于各种功能开关（如随应用启动、阻止更新等）
   *
   * @param context 上下文
   * @param text 开关文字描述
   * @param isChecked 默认是否选中
   * @param bottomMargin 底部间距（dp转换后的值）
   * @return 配置好的Switch
   */
  private static Switch createSwitch(
      Context context, String text, boolean isChecked, int bottomMargin) {
    Switch switchView = new Switch(context);
    LinearLayout.LayoutParams params =
        getLinearLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, bottomMargin);
    switchView.setLayoutParams(params);
    switchView.setText(text);
    switchView.setChecked(isChecked);
    switchView.setGravity(Gravity.CENTER_VERTICAL); // 文字和开关垂直居中对齐
    switchView.setTextSize(15);
    switchView.setTextColor(Color.BLACK);
    switchView.setPadding(0, dpToPx(context, 8), 0, dpToPx(context, 8)); // 增加开关上下内边距，增大点击区域
    return switchView;
  }

  /**
   * 创建忽略电池优化开关 特殊开关，带点击事件处理（跳转到系统电池优化设置页面）
   *
   * @param context 上下文
   * @param sp SP存储工具
   * @return 配置好的Switch
   */
  private static Switch createBatteryOptSwitch(Context context, SPUtils sp) {
    // 检查当前是否已忽略电池优化
    PowerManager powerManager =
        (PowerManager) DragonGlobals.getDragonApplication().getSystemService(Context.POWER_SERVICE);
    boolean isIgnoring =
        powerManager != null
            && powerManager.isIgnoringBatteryOptimizations(
                DragonGlobals.getDragonApplication().getPackageName());

    // 创建开关
    Switch switchView = createSwitch(context, "忽略电池优化", isIgnoring, dpToPx(context, 12));

    // 设置开关状态变化事件
    switchView.setOnCheckedChangeListener(
        (view, isChecked) -> {
          Intent intent;
          if (isChecked) {
            // 申请忽略电池优化
            intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
          } else {
            // 跳转到应用详情页（手动关闭优化）
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
          }
          // 启动设置页面
          // TODO:优化获取Activity方法，避免使用该方法
          AppUtils.getTargetActivity(
                  "com.dragon.read.component.biz.impl.mine.settings.SettingsActivity")
              .startActivity(intent);
        });

    return switchView;
  }

  /**
   * 构建配置对话框 封装对话框的标题、视图、按钮及点击事件（保存配置、取消、导入书源）
   *
   * @param context 上下文
   * @param mainContainer 对话框内容视图
   * @param sp SP存储工具
   * @param portEditText 端口输入框
   * @param debugModeSwitch 调试模式开关
   * @return 构建完成的AlertDialog
   */
  private static AlertDialog buildConfigDialog(
      Context context,
      LinearLayout mainContainer,
      SPUtils sp,
      EditText portEditText) {
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
    dialogBuilder
        .setTitle("FQ Helper 配置")
        .setView(mainContainer)
        // 确定按钮 - 保存配置
        .setPositiveButton("确定", (dialog, which) -> saveConfig(context, sp, portEditText))
        // 取消按钮 - 关闭对话框
        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
        // 中性按钮 - 导入书源
        .setNeutralButton("导入书源", (dialog, which) -> handleImportBookSource(context, sp));

    AlertDialog alertDialog = dialogBuilder.create();
    return alertDialog;
  }

  /**
   * 保存配置参数到SP 验证并保存端口号及各开关状态
   *
   * @param context 上下文
   * @param sp SP存储工具
   * @param portEditText 端口输入框
   * @param debugModeSwitch 调试模式开关
   */
  private static void saveConfig(Context context, SPUtils sp, EditText portEditText) {
    // 验证端口号
    String port = portEditText.getText().toString().trim();
    if (TextUtils.isEmpty(port)) {
      ToastUtils.show("端口号不能为空");
      return;
    }
    // 端口号正则校验（0-65535）
    String portRegex =
        "^(0|[1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
    if (!port.matches(portRegex)) {
      ToastUtils.show("端口号无效，请输入0-65535之间的整数");
      return;
    }

    // 获取各开关状态（从父容器中查找开关控件）
    LinearLayout container = (LinearLayout) portEditText.getParent();
    Switch startWithAppSwitch = (Switch) container.getChildAt(2);
    Switch startWithServiceSwitch = (Switch) container.getChildAt(3);
    Switch blockUpdatesSwitch = (Switch) container.getChildAt(6);
    Switch localVipSwitch = (Switch) container.getChildAt(7);
    Switch debugModeSwitch = (Switch) container.getChildAt(8);

    // 保存配置到SP
    sp.put(SPConstants.PORT, port);
    sp.put(SPConstants.START_WITH_APP, startWithAppSwitch.isChecked());
    sp.put(SPConstants.START_WITH_SERVICE, startWithServiceSwitch.isChecked());
    sp.put(SPConstants.BLOCK_UPDATES, blockUpdatesSwitch.isChecked());
    sp.put(SPConstants.LOCAL_VIP, localVipSwitch.isChecked());
    sp.put(SPConstants.DEBUG_MODE, debugModeSwitch.isChecked());

    ToastUtils.show("配置已保存");

    if (debugModeSwitch.isChecked()) {
      ToastUtils.show("⚠️警告：启用模块调试后，将输出更详细的日志，且响应会以格式化 JSON 返回，可能导致传输速度变慢。");
    }
  }

  /**
   * 处理导入书源逻辑 启动HTTP服务器，生成书源导入链接并跳转到阅读应用
   *
   * @param context 上下文
   * @param sp SP存储工具
   */
  private static void handleImportBookSource(Context context, SPUtils sp) {
    // 启动HTTP服务器（如未启动）
    HttpServer httpServer = HttpServer.getInstance();
    if (!httpServer.isAlive()) {
      try {
        httpServer.start();
      } catch (Throwable t) {
        LogUtils.logE("启动服务失败：", t);
        Toast.makeText(context, "启动服务失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        return;
      }
    }

    // 生成书源导入链接
    String portStr = sp.getString(SPConstants.PORT, SPConstants.DEFAULT_PORT);
    String importUrl =
        "legado://booksource/importonline?src=http://localhost:" + portStr + "/booksource";
    Uri uri = Uri.parse(importUrl);

    // 跳转到阅读应用
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        != null) {
      context.startActivity(intent);
    } else {
      // 未安装阅读应用，跳转到应用市场
      ToastUtils.show("未找到阅读应用，请安装");
      Intent marketIntent =
          new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=io.legado.app"));
      context.startActivity(marketIntent);
    }
  }

  /**
   * 创建LinearLayout布局参数 封装通用布局参数创建逻辑，减少重复代码
   *
   * @param width 宽度（如MATCH_PARENT）
   * @param height 高度（如WRAP_CONTENT）
   * @param bottomMargin 底部间距（px值）
   * @return 配置好的LayoutParams
   */
  private static LinearLayout.LayoutParams getLinearLayoutParams(
      int width, int height, int bottomMargin) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
    params.bottomMargin = bottomMargin;
    return params;
  }

  /**
   * dp转px 将dp单位转换为像素，适配不同屏幕密度
   *
   * @param context 上下文
   * @param dp dp值
   * @return 转换后的px值
   */
  private static int dpToPx(Context context, int dp) {
    return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
  }

  /**
   * 判断当前系统是否处于暗黑（夜间）模式。
   *
   * <p>原理：通过检查当前 Configuration 的 uiMode 字段里是否包含 {@link Configuration#UI_MODE_NIGHT_YES} 标志位来确定。
   *
   * @param context 任意可用的 Context（Application / Activity / Service 均可）
   * @return true —— 系统当前为暗黑模式（Dark/Night Mode） false —— 系统当前为日间模式（Light Mode）
   */
  /*private static boolean isNight(Context context) {
    boolean isNight =
        (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    return isNight;
  }*/
}
