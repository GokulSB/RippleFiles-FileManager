package test;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) {
        System.out.println("Methods in AuthorizationRequest.Builder:");
        for (Method m : AuthorizationRequest.Builder.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
    }
}
