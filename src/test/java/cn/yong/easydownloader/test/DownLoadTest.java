package cn.yong.easydownloader.test;

import cn.yong.easydownloader.AMFDownloader;
import cn.yong.easydownloader.DownLoadResponse;
import cn.yong.easydownloader.DownloadMission;
import cn.yong.easydownloader.Downloader;
import junit.framework.Assert;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import sun.misc.BASE64Decoder;

import java.io.IOException;

/**
 * Created by yong on 2016/3/4.
 */
public class DownLoadTest {

    private static final Logger logger = LogManager.getLogger(DownLoadTest.class);

    /**
     * 下载北京天气预报 soap 协议
     * @throws IOException
     */
    @Test
    public void testDownloadBeijing() throws IOException {
        String url = "http://zx.bjmemc.com.cn/DataService.svc";
        String reqstr_base64= "VgILAXMECwFhBlYIRAoeAIKZHXVybjpEYXRhU2VydmljZS9HZXRXZWJQcmVkaWN0RBqtim+rf48pzEG5+jpiCVtZjkQsRCqrFAFEDB4AgpknaHR0cDovL3p4LmJqbWVtYy5jb20uY24vRGF0YVNlcnZpY2Uuc3ZjAVYOQA1HZXRXZWJQcmVkaWN0AQEB";
        DownloadMission mission = new DownloadMission();
        mission.setContentType("application/soap+msbin1");
        mission.setMethodType("POST");
        mission.setUrl(url);
        mission.setReqContentBytes(new BASE64Decoder().decodeBuffer(reqstr_base64));
        DownLoadResponse resp = Downloader.getInstance().downLoadMissionSync(mission);
        Assert.assertTrue(resp.isSuccess());
        logger.info(resp.getResponseContent());
    }

    @Test
    public void testDownLoadShanghai(){
       Object obj = AMFDownloader.downLoad("http://www.semc.com.cn/aqi/Gateway.aspx", "ServiceLibrary.Sample.getSiteAQIData", 201);//上海
        logger.info(obj);
    }
}
