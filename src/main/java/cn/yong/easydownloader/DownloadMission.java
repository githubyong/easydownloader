package cn.yong.easydownloader;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpMethods;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下载的具体任务
 */
public class DownloadMission implements Cloneable {

    private String url;
    private String contentType;//请求内容类型
    private String methodType;//请求方法类型
    private String referer;//告诉服务器我是从哪个页面链接过来的
    private String cookie;
    private String encode;//编码方式
    private String reqContentStr;//requestContent
    private byte[] reqContentBytes;//requestContent
    private Map<String, String> headerMap;//其他头文件内容
    private String suffix;//文件后缀名

    private AtomicInteger counter = new AtomicInteger();//计数器,下载失败后重试

    private DownFinishCallable downFinishCallable;//下载完成回调接口


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getEncode() {
        return encode;
    }

    public void setEncode(String encode) {
        this.encode = encode;
    }

    public String getReqContentStr() {
        return reqContentStr;
    }

    public void setReqContentStr(String reqContentStr) {
        this.reqContentStr = reqContentStr;
    }

    public byte[] getReqContentBytes() {
        return reqContentBytes;
    }

    public void setReqContentBytes(byte[] reqContentBytes) {
        this.reqContentBytes = reqContentBytes;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public Map<String, String> getHeaderMap() {
        if (headerMap == null) {
            headerMap = new HashMap<>();
        }
        return headerMap;
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    public AtomicInteger getCounter() {
        return counter;
    }

    public DownFinishCallable getDownFinishCallable() {
        return downFinishCallable;
    }

    public void setDownFinishCallable(DownFinishCallable downFinishCallable) {
        this.downFinishCallable = downFinishCallable;
    }

    @Override
    public DownloadMission clone() throws CloneNotSupportedException {
        return (DownloadMission) super.clone();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("mission[");
        sb.append("url:").append(this.url).append(",");
        sb.append("Content-Type:").append(this.contentType).append(",");
        sb.append("methodType:").append(this.methodType).append(",");
//        sb.append("reqContentStr:").append(this.reqContentStr).append(",");
        sb.append("referer:").append(this.referer);
        sb.append("]");
        return sb.toString();
    }
}
