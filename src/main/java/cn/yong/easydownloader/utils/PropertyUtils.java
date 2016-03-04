package cn.yong.easydownloader.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置文件属性获取工具类
 */
public class PropertyUtils {
    private static Properties properties = new Properties();

    static {
        try {
//            InputStream in = ClassUtils.getClassLoader().getResourceAsStream("conf/conf.properties");
            InputStream in = new FileInputStream(new File("conf/conf.properties"));
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        String val = properties.getProperty(key);
        return (val == null) ? defaultValue : val;
    }
}
