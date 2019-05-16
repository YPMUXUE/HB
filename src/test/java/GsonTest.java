import com.google.gson.Gson;
import com.google.gson.JsonObject;
import common.ResourceLoader;
import common.resource.loader.ConfigProperties;

public class GsonTest {
    public static void main(String[] args) {
        String ip = ConfigProperties.getProperty("ip");
        System.out.println(ip);
    }
}
