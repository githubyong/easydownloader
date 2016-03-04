package cn.yong.easydownloader.utils;



import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassUtils {
    private static final Logger logger = LogManager.getLogger(ClassUtils.class);

    /**
     * 获取指定package下的所有class
     *
     * @param packageName 包名
     * @param loop        是否迭代
     * @return
     * @throws Exception
     */
    public static List<Class> getClasses(String packageName, boolean loop) throws Exception {
        Enumeration<URL> e = getClassLoader().getResources(packageName.replace(".", "/"));
        List<Class> classes = new ArrayList<>();
        while (e.hasMoreElements()) {
            URL url = e.nextElement();
            if ("file".equals(url.getProtocol())) {
                String packagePath = URLDecoder.decode(url.getFile(), "UTF-8");
                File file = new File(packagePath);
                classes.addAll(getClasses(file, packageName, loop));
            }
        }
        return classes;
    }

    private static List<Class> getClasses(File dir, String packageName, boolean loop) throws Exception {
        List<Class> list = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory() && loop) {
                list.addAll(getClasses(file, packageName + "." + file.getName(), loop));
            } else {
                if (file.getName().endsWith(".class")) {
                    Class clz = Class.forName(packageName + "." + file.getName().replace(".class", ""));
                    list.add(clz);
                }
            }
        }
        return list;
    }

    /**
     * @return 当前线程的class loader
     */
    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 获得class loader<br>
     * 若当前线程class loader不存在，取当前类的class loader
     *
     * @return 类加载器
     */
    public static ClassLoader getClassLoader() {
        ClassLoader classLoader = getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassUtils.class.getClassLoader();
        }
        return classLoader;
    }
}
