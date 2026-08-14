import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

public class TestReflection {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        URL url = new URL("file:///" + jarPath.replace("\\", "/"));
        URLClassLoader loader = new URLClassLoader(new URL[]{url});
        Class<?> clazz = loader.loadClass("com.google.android.gms.auth.api.identity.AuthorizationResult");
        System.out.println("Methods in AuthorizationResult:");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
        System.out.println("\nMethods in AuthorizationRequest.Builder:");
        Class<?> builderClazz = loader.loadClass("com.google.android.gms.auth.api.identity.AuthorizationRequest$Builder");
        for (Method m : builderClazz.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
    }
}
