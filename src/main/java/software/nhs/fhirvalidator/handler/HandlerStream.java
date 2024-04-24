package software.nhs.fhirvalidator.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.JsonElement;
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
            manifest_file = "nhs_digital.manifest.json";
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
            JsonObject jsonObject = JsonParser.parseString(rawInput).getAsJsonObject();
            JsonObject headers = jsonObject.get("headers").getAsJsonObject();
            String xRequestID = headers.get("x-request-id").isJsonNull() ? "" : headers.get("x-request-id").getAsString();
            String nhsdCorrelationID = headers.get("nhsd-correlation-id").isJsonNull() ? "" : headers.get("nhsd-correlation-id").getAsString();
            String nhsdRequestID = headers.get("nhsd-request-id").isJsonNull() ? "" : headers.get("nhsd-request-id").getAsString();
            String xCorrelationID = headers.get("x-correlation-id").isJsonNull() ? "" : headers.get("x-correlation-id").getAsString();
            String apigwRequestID = headers.get("apigw-request-id").isJsonNull() ? "" : headers.get("apigw-request-id").getAsString();
            LoggingUtils.appendKey("x-request-id", xRequestID);
            LoggingUtils.appendKey("nhsd-correlation-id", nhsdCorrelationID);
            LoggingUtils.appendKey("nhsd-request-id", nhsdRequestID);
            LoggingUtils.appendKey("x-correlation-id", xCorrelationID);
            LoggingUtils.appendKey("apigw-request-id", apigwRequestID);
            log.info("Got all the headers");
            String validatorResult = validateController.validate(jsonObject.get("body").toString());


            try (PrintWriter writer = new PrintWriter(outputStream)) {
                writer.print(validatorResult);
            }

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("error in handleRequest", ex);
        }
    }
}
