package software.nhs.FHIRValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Utils {
    public static String getResourceContent(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(resource);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            String rawData = result.toString("UTF-8");
            return rawData;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
