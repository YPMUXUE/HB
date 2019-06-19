package priv.common;


import java.io.InputStream;
import java.net.URL;

public class ResourceLoader {
	private static final ClassLoader CLASS_LOADER;

	static {
		CLASS_LOADER = ResourceLoader.class.getClassLoader();
	}
	public static InputStream getResource(String path){
		return CLASS_LOADER.getResourceAsStream(path);
	}
	public static URL getURL(String path){
		return CLASS_LOADER.getResource(path);
	}
}
