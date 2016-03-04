package cn.yong.easydownloader.utils;

import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.http.HttpStatus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Scanner;

/**
 * 通过urlconnection 测试代理连接情况
 */
public class ProxyUtils {

    /**
     * 通过urlconnection 测试代理连接情况
     *
     * @param address
     * @return
     */
    public static boolean testProxyByUrlConnection(Address address) {
        try {
            String proxy_url = PropertyUtils.getProperty("proxy_url", "http://apis.baidu.com/heweather/weather/free");
            URL url = new URL(proxy_url);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(address.getHost(), address.getPort()));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            int proxy_test_time_out = Integer.valueOf(PropertyUtils.getProperty("proxy_test_time_out", "10000"));
            connection.setConnectTimeout(proxy_test_time_out);
            connection.setReadTimeout(proxy_test_time_out);
            connection.connect();
            return HttpStatus.OK_200 == connection.getResponseCode();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean testProxy(String addr) {
        String[] arr = addr.split(":");
        return testProxyByUrlConnection(new Address(arr[0].trim(), Integer.valueOf(arr[1].trim())));
    }
}
