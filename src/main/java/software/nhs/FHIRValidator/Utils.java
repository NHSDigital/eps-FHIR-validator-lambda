package software.nhs.FHIRValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Utils {
    static Logger log = LogManager.getLogger(Utils.class);

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

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("error in getResourceContent", ex);
        }
    }
}
