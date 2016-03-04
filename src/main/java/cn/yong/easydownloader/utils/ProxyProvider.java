package cn.yong.easydownloader.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 代理提供者 <br/>
 * 第一次获取代理时从文件中把可用代理读入内存，随机获取一个；当可用代理数小于配置的安全代理个数时自动重新从文件读取
 */
public class ProxyProvider {
    private static final Logger logger = LogManager.getLogger(ProxyProvider.class);
    private static final Logger PROXY_LOG = LogManager.getLogger("down_proxy");
    private static List<Address> proxyList = new ArrayList<Address>();

    static List<Address> getProxiesFromFile() {
        List<Address> list = new ArrayList<>();
        try {
            Path path = Paths.get("conf/proxy.txt");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] arr = line.split(":");
                list.add(new Address(arr[0].trim(), Integer.valueOf(arr[1].trim())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static final int EMPTY = 0;//未初始化
    private static final int REFRESH_ING = 1;//正在刷新
    private static final int REFRESH_ED = 2;//已刷新
    private static int STATUS = EMPTY;
    static final LinkedBlockingDeque<Runnable> deque = new LinkedBlockingDeque<Runnable>();
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 200, 5, TimeUnit.SECONDS, deque);

    static synchronized void refreshProxy() {
        STATUS = REFRESH_ING;
        List<Address> proxies = getProxiesFromFile();
        if (executor == null || executor.isShutdown()) {
            executor = new ThreadPoolExecutor(100, 200, 5, TimeUnit.SECONDS, deque);
        }
        for (final Address proxy : proxies) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    boolean available = ProxyUtils.testProxyByUrlConnection(proxy);
                    if (available && !proxyList.contains(proxy)) {
                        proxyList.add(proxy);
                    }
                }
            });
        }
        new Thread(new ExcutorCloseRunnable()).start();//等待测试完成关闭线程池
    }

    /**
     * 监控excutor线程池，如果所有的连接测试都结束，关闭线程池
     */
    static class ExcutorCloseRunnable implements Runnable {
        @Override
        public void run() {
            while (executor.getActiveCount() > 0) {
                try {
                    logger.info(executor + " availableProxy=" + proxyList.size());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            STATUS = REFRESH_ED;
            executor.shutdown();
            logger.info("shut down excutor ============================" + executor.toString() + " availableProxy=" + proxyList.size());
            int safe_proxy_num = Integer.valueOf(PropertyUtils.getProperty("safe_proxy_num"));
            if (proxyList.size() < safe_proxy_num) {
                PROXY_LOG.info(MessageFormat.format("########### 可用代理不足，请更新代理，可用的有{0}",proxyList));
            }
            //TODO 如果测试完成时可用代理小于安全代理，发邮件通知更新代理文件
        }
    }


    private static Random random = new Random();

    public static synchronized Address getRandomProxy() {
        if (STATUS == EMPTY) {
            refreshProxy();
            try {
                while (proxyList.size() < 1) {
                    Thread.sleep(500);//等待2秒后 再尝试获取
                }
                return getRandomProxy();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (proxyList.size() > 0) {
            int safe_proxy_num = Integer.valueOf(PropertyUtils.getProperty("safe_proxy_num", "10"));
            if (STATUS == REFRESH_ED && proxyList.size() < safe_proxy_num) {//如果小于安全代理个数，刷新
                refreshProxy();
            }
            int ran = random.nextInt(proxyList.size());
            return proxyList.get(ran);
        } else {
            return getRandomProxy();
        }
    }

    public static synchronized void expireProxy(Address address) {
        proxyList.remove(address);
    }

}
