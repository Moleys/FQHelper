package com.xxhy.fqhelper.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 网络工具类
 * 提供IP地址相关的获取功能
 */
public class NetworkUtils {

    /**
     * 获取设备的IP地址（IPv4或IPv6）
     * <p>注意：需要在AndroidManifest中声明 INTERNET 权限：
     * {@code <uses-permission android:name="android.permission.INTERNET" />}</p>
     *
     * @param useIPv4 是否优先返回IPv4地址（true返回IPv4，false返回IPv6）
     * @return 有效的IP地址字符串；若未获取到则返回"localhost"
     */
    public static String getIPAddress(final boolean useIPv4) {
        try {
            // 遍历所有网络接口（如以太网、WiFi、移动数据等）
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            // 存储所有有效的IP地址（按接口遍历顺序，优先添加最新获取的地址）
            List<InetAddress> inetAddresses = new ArrayList<>();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // 过滤非活动接口和回环接口（避免获取无效/本地测试地址，如小米设备可能返回的10.0.2.15）
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                // 遍历当前网络接口下的所有IP地址
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    // 将地址添加到列表头部，使后获取的地址优先被检查
                    inetAddresses.add(0, addresses.nextElement());
                }
            }

            // 遍历IP地址列表，筛选目标类型的有效地址
            for (InetAddress address : inetAddresses) {
                // 跳过回环地址（如127.0.0.1、::1）
                if (address.isLoopbackAddress()) {
                    continue;
                }

                String hostAddress = address.getHostAddress();
                if (hostAddress == null) {
                    continue;
                }

                // 判断是否为IPv4地址（IPv4无冒号，IPv6有冒号）
                boolean isIPv4 = hostAddress.indexOf(':') < 0;

                // 根据需求返回对应类型的IP
                if (useIPv4) {
                    if (isIPv4) {
                        return hostAddress;
                    }
                } else {
                    if (!isIPv4) {
                        // 处理IPv6地址中的区域标识（如fe80::1%wlan0中的%及后面内容）
                        return processIPv6Address(hostAddress);
                    }
                }
            }

        } catch (SocketException e) {
            // 捕获网络接口访问异常（如权限不足、网络未就绪等）
            e.printStackTrace();
        }

        // 未获取到有效IP时，返回本地主机名
        return "localhost";
    }

    /**
     * 处理IPv6地址，移除可能的区域标识
     *
     * @param rawIPv6 原始IPv6地址字符串
     * @return 处理后的标准IPv6地址
     */
    private static String processIPv6Address(String rawIPv6) {
        int zoneIndex = rawIPv6.indexOf('%');
        if (zoneIndex > 0) {
            // 截取%之前的部分作为有效IPv6地址
            rawIPv6 = rawIPv6.substring(0, zoneIndex);
        }
        return rawIPv6;
    }
}
