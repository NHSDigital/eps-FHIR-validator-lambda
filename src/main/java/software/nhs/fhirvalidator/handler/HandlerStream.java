package software.nhs.fhirvalidator.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.nhs.fhirvalidator.controller.ValidateController;
import software.nhs.fhirvalidator.util.ResourceUtils;

public class HandlerStream implements RequestStreamHandler {

    private final ValidateController validateController;
    Logger log = LogManager.getLogger(HandlerStream.class);

    public HandlerStream() {
        log.info("Creating the Validator instance for the first time...");
        String manifest_file = System.getenv("PROFILE_MANIFEST_FILE");
        if (manifest_file == null) {
            manifest_file = "uk_core.manifest.json";
        }
        log.info(String.format("Using manifest file : %s", manifest_file));

        validateController = new ValidateController(manifest_file);

        log.info("Validating once to force the loading of all the validator related classes");
        String primerPayload = ResourceUtils.getResourceContent("primerPayload.json");
        validateController.validate(primerPayload);

        log.info("Validator is ready");
    }

    @Logging(clearState = true)
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            String rawInput = result.toString();
            log.info(rawInput);
            JsonObject jsonPayload = JsonParser.parseString(rawInput)
                .getAsJsonObject().get("parameters")
                .getAsJsonObject().get("Payload")
                .getAsJsonObject();
            JsonObject headers = jsonPayload.get("headers").getAsJsonObject();
            String xRequestID = headers.get("x-request-id") == null ? "" : headers.get("x-request-id").getAsString();
            LoggingUtils.appendKey("x-request-id", xRequestID);
            String nhsdCorrelationID = headers.get("nhsd-correlation-id") == null ? "" : headers.get("nhsd-correlation-id").getAsString();
            LoggingUtils.appendKey("nhsd-correlation-id", nhsdCorrelationID);
            String nhsdRequestID = headers.get("nhsd-request-id") == null ? "" : headers.get("nhsd-request-id").getAsString();
            LoggingUtils.appendKey("nhsd-request-id", nhsdRequestID);
            String xCorrelationID = headers.get("x-correlation-id") == null ? "" : headers.get("x-correlation-id").getAsString();
            LoggingUtils.appendKey("x-correlation-id", xCorrelationID);
            String apigwRequestID = headers.get("apigw-request-id") == null ? "" : headers.get("apigw-request-id").getAsString();
            LoggingUtils.appendKey("apigw-request-id", apigwRequestID);
 
            log.info("Calling validate function");
            String validatorResult = validateController.validate(jsonPayload.get("body").toString());
            log.info(validatorResult);

            try (PrintWriter writer = new PrintWriter(outputStream)) {
                writer.print(validatorResult);
            }

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("error in handleRequest", ex);
        }
    }
}
