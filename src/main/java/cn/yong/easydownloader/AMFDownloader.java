package cn.yong.easydownloader;

import cn.yong.easydownloader.utils.PropertyUtils;
import cn.yong.easydownloader.utils.ProxyProvider;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.Address;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 获取amf协议的数据
 */
public class AMFDownloader {
    private static final Logger logger = LogManager.getLogger(AMFDownloader.class);


    public static Object downLoad(String url, String method, Object... args) {
        AMFConnection amfConnection = new AMFConnection();
        AtomicInteger counter = new AtomicInteger();
        Object obj = callWithProxyByCounter(amfConnection, counter, url, method, args);
        amfConnection.close();
        return obj;
    }

    /**
     * 使用代理进行下载，失败后重试，最多尝试下载5次
     *
     * @param amfConnection
     * @param counter
     * @param url
     * @param method
     * @param args
     * @return
     */
    private static Object callWithProxyByCounter(AMFConnection amfConnection, AtomicInteger counter, String url, String method, Object... args) {
        try {
            Address proxyAddress = null;
            boolean use_proxy = "true".equalsIgnoreCase(PropertyUtils.getProperty("use_proxy"));
            if (use_proxy) {
                proxyAddress = ProxyProvider.getRandomProxy();
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress.getHost(), proxyAddress.getPort()));
                amfConnection.setProxy(proxy);
            }
            logger.info(MessageFormat.format("第{0}次下载 url={1} method={2} proxy={3}", counter.get(), url, method, proxyAddress));
            amfConnection.connect(url);
            Object obj = amfConnection.call(method, args);
            return obj;
        } catch (ClientStatusException | ServerStatusException e) {
            if (counter.incrementAndGet() <= 5) {
                return callWithProxyByCounter(amfConnection, counter, url, method, args);
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }
}
