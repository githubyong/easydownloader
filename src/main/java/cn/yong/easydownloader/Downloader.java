package cn.yong.easydownloader;

import cn.yong.easydownloader.utils.PropertyUtils;
import cn.yong.easydownloader.utils.ProxyProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2016/2/18 0018.
 */
public class Downloader {

    private static final Logger logger = LogManager.getLogger(Downloader.class);
    private static final Logger PROXY_LOG = LogManager.getLogger("down_proxy");
    private static final String PROXY_ERR = "Proxy Error";//代理错误时的响应信息包含的内容如<title>502 Proxy Error</title>

    private HttpClient client;

    private boolean use_proxy;

    private Downloader() throws Exception {
        use_proxy = "true".equalsIgnoreCase(PropertyUtils.getProperty("use_proxy"));
        client = new HttpClient();
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.setMaxConnectionsPerAddress(200); // max 200 concurrent connections to every address
        client.setThreadPool(new QueuedThreadPool(200)); // max 250 threads
        client.setTimeout(20 * 1000); // 10 seconds timeout; if no server reply, the request expires
        client.start();
        if (use_proxy) {
            client.setProxy(ProxyProvider.getRandomProxy());
        }
    }

    public static Downloader getInstance() {
        return Manager.downloader;
    }

    private static class Manager {
        private static Downloader downloader;

        static {
            try {
                downloader = new Downloader();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 异步执行下载任务，需要mission有回调接口<br/>
     *
     * @param mission
     * @return
     */
    public void downLoadMissionAsync(final DownloadMission mission) {
        logger.info("下载任务[异步]:" + mission);
        final DownFinishCallable callable = mission.getDownFinishCallable();
        ContentExchange exchange = new ContentExchange(true) {
            @Override
            protected void onConnectionFailed(Throwable x) {//连接失败
                super.onConnectionFailed(x);
                retryIfAllowed("连接失败/onConnectionFailed");
            }

            @Override
            protected void onException(Throwable x) {
                super.onException(x);
            }

            @Override
            protected void onExpire() {//超时
                super.onExpire();
                retryIfAllowed("超时/onConnectionFailed");
            }

            @Override
            protected void onResponseComplete() {
                try {
                    byte[] bytes = getResponseContentBytes();
                    int status = getResponseStatus();
                    if (HttpStatus.OK_200 == status) {
                        callFinish(new DownLoadResponse(true, status, "下载成功", getResponseContent(), bytes));
                    } else {
                        String failureCause = fetchFailureCause(mission, this);
                        retryIfAllowed(MessageFormat.format("{0} (status={1})", failureCause, status));
                    }
                } catch (Exception e) {
                    logger.error("处理任务结束时失败:", e);
                }
            }

            /**
             * 在设置的允许重试的范围内尝试重新下载
             */
            void retryIfAllowed(String cause) {
                try {
                    Integer retry_num = Integer.valueOf(PropertyUtils.getProperty("retry_num", "5"));
                    Integer count = mission.getCounter().incrementAndGet();
                    logger.warn(MessageFormat.format("[{0}]  当前第{1}次下载失败[{2}]，准备重试 ", mission, count, cause));
                    if (count < retry_num) {
                        boolean ssl = HttpSchemes.HTTPS_BUFFER.equalsIgnoreCase(getScheme());
                        HttpDestination destination = client.getDestination(getAddress(), ssl);
                        client.removeDestination(destination);
                        if (use_proxy) {
                            ProxyProvider.expireProxy(client.getProxy());
                            client.setProxy(ProxyProvider.getRandomProxy());
                        }
                        downLoadMissionAsync(mission);
                    } else {
                        callFinish(new DownLoadResponse(false, getResponseStatus(), cause, null, null));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            void callFinish(DownLoadResponse response) {
                if (callable != null) {
                    try {
                        callable.call(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.error(mission + " 没有回调接口");
                }
            }
        };

        try {
            setExchangeParam(exchange, mission);
            client.send(exchange);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public DownLoadResponse downLoadMissionSync(DownloadMission mission) {
        try {
            logger.info("下载任务[同步]:" + mission);
            ContentExchange exchange = new ContentExchange(false);
            setExchangeParam(exchange, mission);
            client.send(exchange);
            int status = exchange.waitForDone();
            int httpStatus = exchange.getResponseStatus();
            if (HttpExchange.STATUS_COMPLETED == status) {//下载完成
                if (HttpStatus.OK_200 == httpStatus) {//success
                    return new DownLoadResponse(true, httpStatus, "下载成功", exchange.getResponseContent(), exchange.getResponseContentBytes());
                } else {
                    String failureCause = fetchFailureCause(mission, exchange);
                    return retrySyncDownloadIfAllowed(mission, exchange, httpStatus, failureCause);
                }
            } else {
                return retrySyncDownloadIfAllowed(mission, exchange, httpStatus, "未完成-" + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DownLoadResponse retrySyncDownloadIfAllowed(DownloadMission mission, ContentExchange exchange, int httpStatus, String cause) throws Exception {
        Integer retry_num = Integer.valueOf(PropertyUtils.getProperty("retry_num", "5"));
        Integer count = mission.getCounter().incrementAndGet();
        if (count < retry_num) {//准备重试
            logger.warn(MessageFormat.format("[{0}]  当前第{1}次下载失败[{2}]，准备重试 ", mission, count, cause));
            boolean ssl = HttpSchemes.HTTPS_BUFFER.equalsIgnoreCase(exchange.getScheme());
            HttpDestination destination = client.getDestination(exchange.getAddress(), ssl);
            client.removeDestination(destination);
            if (use_proxy) {
                ProxyProvider.expireProxy(client.getProxy());
                client.setProxy(ProxyProvider.getRandomProxy());
            }
            return downLoadMissionSync(mission);
        } else {
            return new DownLoadResponse(false, httpStatus, cause, null, null);
        }
    }


    /**
     * 设置exchange的一些属性
     *
     * @param exchange
     * @param mission
     */
    private void setExchangeParam(ContentExchange exchange, DownloadMission mission) {
        exchange.setURL(mission.getUrl());
        if (StringUtils.isNotBlank(mission.getMethodType())) {
            exchange.setMethod(mission.getMethodType());
        }
        String reqContentStr = mission.getReqContentStr();
        byte[] reqContentBytes = mission.getReqContentBytes();
        if (reqContentBytes != null) {//在构造mission时如果content不为空，尝试转化为解密(base64)后的bytes,所以此处反过来，如果bytes不为空，设为requestContent；否则再用正常的resStr
            Buffer requestContent = new ByteArrayBuffer(reqContentBytes);
            exchange.setRequestContent(requestContent);
        } else if (StringUtils.isNotBlank(reqContentStr)) {
            Buffer requestContent = new ByteArrayBuffer(reqContentStr);
            exchange.setRequestContent(requestContent);
        }
        if (StringUtils.isNotBlank(mission.getContentType())) {
            exchange.setRequestHeader("Content-Type", mission.getContentType());
        }
        for (String key : mission.getHeaderMap().keySet()) {//其他头文件内容
            exchange.setRequestHeader(key, mission.getHeaderMap().get(key));
        }
    }


    /**
     * 获取失败原因
     *
     * @param exchange
     * @return
     */
    private String fetchFailureCause(DownloadMission mission, ContentExchange exchange) {
        byte[] data = exchange.getResponseContentBytes();
        String cause = null;
        String respStr = new String(data);
        try {
            String reg = "charset=*.+\"";//eg:charset=GB2312">
            Pattern p = Pattern.compile(reg);
            Matcher matcher = p.matcher(respStr);
            if (matcher.find()) {
                String charStr = matcher.group();
                String charSet = charStr.substring(charStr.indexOf("=") + 1, charStr.indexOf("\""));
                respStr = new String(data, charSet);
                if (StringUtils.containsIgnoreCase(respStr, "<title>")) {
                    cause = respStr.substring(StringUtils.indexOfIgnoreCase(respStr, "<title>") + 7, StringUtils.indexOfIgnoreCase(respStr, "</title>"));
                    if (StringUtils.contains(cause, PROXY_ERR)) {
                        PROXY_LOG.warn(MessageFormat.format("代理错误{0}, 下载的任务{1}", client.getProxy(), mission));
                    }
                } else {
                    logger.error("UNKNOWN_ERROR:" + respStr);
                    cause = "UNKNOWN_ERROR:" + exchange.getResponseStatus();
                }
            } else {
                cause = "UNKNOWN_ERROR:" + exchange.getResponseStatus();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cause;
    }

}
