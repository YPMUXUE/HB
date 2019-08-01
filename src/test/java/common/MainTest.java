package common;

import java.net.URL;
import java.util.Enumeration;

public class MainTest {
    private static final String x1="123";
    private static final int x2=1;
    public static void main(String[] args) throws Exception {
        ClassLoader cl = MainTest.class.getClassLoader();
        Enumeration<URL> resources = cl.getResources("config.properties");
        while (resources.hasMoreElements()){
            URL url = resources.nextElement();
            System.out.println(url);
        }
    }
}
