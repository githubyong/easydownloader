package cn.yong.easydownloader;

/**
 * 下载完成回调接口
 */
public interface DownFinishCallable<V> {
    V call(DownLoadResponse response) throws Exception;
}
