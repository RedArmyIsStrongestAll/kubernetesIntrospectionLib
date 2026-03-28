package entities.services.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class TestUtils {
    public static String loadRbacYaml(String filename) throws IOException {
        URL resource = TestUtils.class.getClassLoader().getResource(filename);
        if (resource != null) {
            File file = new File(resource.getFile());
            if (file.exists()) {
                return new String(Files.readAllBytes(file.toPath()));
            }
        }
        throw new IOException("No " + filename + " file found");
    }
}
