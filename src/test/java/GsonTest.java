import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class GsonTest {
    public static void main(String[] args) {
        Gson gson=new Gson();
        JsonObject jsonObject=gson.fromJson("{\"credentials\":\"omit\",\"headers\":{\"upgrade-insecure-requests\":\"1\"},\"referrerPolicy\":\"no-referrer-when-downgrade\",\"body\":null,\"method\":\"GET\",\"mode\":\"cors\"}",JsonObject.class);
         System.out.println(jsonObject);
    }
}
