import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import java.io.File;
import java.io.InputStream;

public class TestRar {
    public static void main(String[] args) throws Exception {
        Archive a1 = new Archive(new File(""));
        Archive a2 = new Archive((InputStream)null);
        Archive a3 = new Archive((InputStream)null, "pass");
    }
}
