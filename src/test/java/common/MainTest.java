package common;

import java.net.URL;
import java.util.Enumeration;

public class MainTest {
    public static void main(String[] args) throws Exception {
        ClassLoader cl = MainTest.class.getClassLoader();
        Enumeration<URL> resources = cl.getResources("config.properties");
        while (resources.hasMoreElements()){
            URL url = resources.nextElement();
            System.out.println(url);
        }
    }
}
