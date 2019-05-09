package common.resource.loader;


import common.ResourceLoader;
import common.log.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigProperties {
	private static final Map<String, String> configMap;

	static {
		InputStream inputStream = ResourceLoader.getResource("config.properties");

		Map<String, String> tempMap = new HashMap<>();
		try {
			Properties properties;
			properties = new Properties();
			properties.load(inputStream);
			properties.forEach((k, v) -> {
				tempMap.put((String) k, (String) v);
				LogUtil.info(() -> "load config k:" + k + ",v:" + v);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		configMap = Collections.unmodifiableMap(tempMap);
	}

	public static String getProperty(String key) {
		return configMap.get(key);
	}
}
