package priv.common.resource;

import priv.common.ResourceLoader;
import priv.common.log.LogUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

public class StaticConfig {
    /**address config**/
    public static String PROXY_SERVER_ADDRESS = null;
    public static int PROXY_SERVER_PORT = 0;
    public static int timeout=120;

    public static String LOCAL_HOST_ADDRESS="127.0.0.1";
    public static int LOCAL_HOST_PORT=9001;

    static {
        try (InputStream resource = ResourceLoader.getResource("config.properties")) {
            if (resource == null) {
                throw new RuntimeException("找不到config.properties");
            }
            Properties properties = new Properties();
            properties.load(resource);
            Field[] declaredFields = StaticConfig.class.getDeclaredFields();
            for (Field field : declaredFields) {
                String name = field.getName();
                String property = properties.getProperty(name, "");
                if (StringUtils.isNotEmpty(property)) {
                    LogUtil.info(() -> "load property:" + name + ":" + property);
                    Class<?> type = field.getType();
                    if (type.isPrimitive()) {
                        if (java.lang.Boolean.TYPE.equals(type)) {
                            field.set(null, Boolean.parseBoolean(property));
                        }
                        if (java.lang.Character.TYPE.equals(type)) {
                            field.set(null, property.charAt(0));
                        }
                        if (java.lang.Byte.TYPE.equals(type)) {
                            field.set(null, Byte.valueOf(property));
                        }
                        if (java.lang.Short.TYPE.equals(type)) {
                            field.set(null, Short.valueOf(property));
                        }
                        if (java.lang.Integer.TYPE.equals(type)) {
                            field.set(null, Integer.valueOf(property));
                        }
                        if (java.lang.Long.TYPE.equals(type)) {
                            field.set(null, Long.valueOf(property));
                        }
                        if (java.lang.Float.TYPE.equals(type)) {
                            field.set(null, Float.valueOf(property));
                        }
                        if (java.lang.Double.TYPE.equals(type)) {
                            field.set(null, Double.valueOf(property));
                        }
                        if (java.lang.Void.TYPE.equals(type)) {
                            //do nothing
                        }
                    } else {
                        field.set(null, property);
                    }
                }
            }
        } catch (Throwable e) {
            LogUtil.error(() -> LogUtil.stackTraceToString(e));
            System.exit(5);
        }

    }

}
