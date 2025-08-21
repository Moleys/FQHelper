package com.xxhy.fqhelper.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段名命名风格转换工具类
 * 提供驼峰命名与下划线命名之间的相互转换
 */
public class FieldNameUtils {

    /**
     * 驼峰转下划线的正则表达式
     * 匹配非首字符的大写字母或数字（使用正向预查确保前面有字符，避免首字符被匹配）
     */
    private static final Pattern HUMP_PATTERN = Pattern.compile("(?<=.)[A-Z0-9]");

    /**
     * 将驼峰命名风格的字符串转换为下划线命名风格
     * 例如：userName → user_name；User2Name → user_2_name
     *
     * @param param 待转换的驼峰风格字符串（null或空字符串返回空）
     * @return 转换后的下划线风格字符串
     */
    public static String humpToUnderline(String param) {
        if (param == null || param.isEmpty()) {
            return "";
        }
        // 匹配非首字符的大写字母/数字，替换为"_+小写"
        Matcher matcher = HUMP_PATTERN.matcher(param);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            // 将匹配到的字符转为小写并在前面加下划线
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        // 拼接剩余部分
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 下划线字符常量
     */
    private static final char UNDERLINE = '_';

    /**
     * 将下划线命名风格的字符串转换为驼峰命名风格
     * 例如：user_name → userName；user__name → userName；_user_name → UserName
     *
     * @param param 待转换的下划线风格字符串（null、空字符串或纯空白返回空）
     * @return 转换后的驼峰风格字符串
     */
    public static String underlineToCamel(String param) {
        // 处理空值或纯空白字符串
        if (param == null || param.trim().isEmpty()) {
            return "";
        }
        int length = param.length();
        StringBuilder sb = new StringBuilder(length);
        int i = 0;
        while (i < length) {
            char currentChar = param.charAt(i);
            // 统一转为小写处理（兼容大写字母的下划线命名）
            currentChar = Character.toLowerCase(currentChar);

            if (currentChar == UNDERLINE) {
                // 遇到下划线，跳过当前下划线，将下一个字符转为大写（若存在）
                if (++i < length) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                // 非下划线字符直接拼接
                sb.append(currentChar);
            }
            i++;
        }
        return sb.toString();
    }
}
