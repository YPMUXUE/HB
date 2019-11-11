package priv.common;


import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
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
	public static byte[] getBytes(String path) throws Exception{
		InputStream stream = CLASS_LOADER.getResourceAsStream(path);
		if (stream == null){
			throw new FileNotFoundException(path);
		}
		return IOUtils.toByteArray(stream);
	}
	public static URL getURL(String path){
		return CLASS_LOADER.getResource(path);
	}
}
