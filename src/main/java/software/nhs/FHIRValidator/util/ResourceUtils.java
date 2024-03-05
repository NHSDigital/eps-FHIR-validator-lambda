package software.nhs.FHIRValidator.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceUtils {
    static Logger log = LogManager.getLogger(ResourceUtils.class);

    public static String getResourceContent(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            ByteArrayOutputStream result;
            try (InputStream inputStream = loader.getResourceAsStream(resource)) {
                result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];

                for (int length; (length = inputStream.read(buffer)) != -1;) {
                    result.write(buffer, 0, length);
                }
            }
            String rawData = result.toString("UTF-8");
            return rawData;

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("error in getResourceContent", ex);
        }
    }

}
