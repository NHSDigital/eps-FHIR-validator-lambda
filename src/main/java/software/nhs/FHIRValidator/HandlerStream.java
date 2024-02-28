package software.nhs.FHIRValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.Logging;

public class HandlerStream implements RequestStreamHandler {

    private final Validator validator;
    Logger log = LogManager.getLogger(HandlerStream.class);

    public HandlerStream() {
        log.info("Creating the Validator instance for the first time...");

        validator = new Validator();

        log.info("Validating once to force the loading of all the validator related classes");
        String primerPayload = Utils.getResourceContent("primerPayload.json");
        validator.validate(primerPayload);

        log.info("Validator is ready");
    }

    @Logging
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            String rawInput = result.toString("UTF-8");
            log.info(rawInput);

            ValidatorResponse validate = validator.validate(rawInput);

            try (PrintWriter writer = new PrintWriter(outputStream)) {
                writer.print(new Gson().toJson(validate));
                writer.close();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
