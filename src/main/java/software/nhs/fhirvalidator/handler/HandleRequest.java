package software.nhs.fhirvalidator.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.nhs.fhirvalidator.controller.ValidateController;
import software.nhs.fhirvalidator.util.ResourceUtils;

public class HandleRequest implements RequestHandler<APIGatewayProxyRequestEvent, String> {

    private final ValidateController validateController;
    Logger log = LogManager.getLogger(HandleRequest.class);

    public HandleRequest() {
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

    @Logging(clearState = true, correlationIdPath = "/headers/x-request-id", logEvent = true)
    @Override
    public String handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LoggingUtils.appendKey("x-request-id", input.getHeaders().get("x-request-id"));
        LoggingUtils.appendKey("nhsd-correlation-id", input.getHeaders().get("nhsd-correlation-id"));
        LoggingUtils.appendKey("nhsd-request-id", input.getHeaders().get("nhsd-request-id"));
        LoggingUtils.appendKey("x-correlation-id", input.getHeaders().get("x-correlation-id"));
        LoggingUtils.appendKey("apigw-request-id", input.getHeaders().get("apigw-request-id"));
        String validatorResult = validateController.validate(input.getBody());
        return validatorResult;
    }
}
