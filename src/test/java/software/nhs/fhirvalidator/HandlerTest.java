package software.nhs.fhirvalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.altindag.log.LogCaptor;
import software.nhs.fhirvalidator.handler.HandlerStream;
import software.nhs.fhirvalidator.util.ResourceUtils;
import ca.uhn.fhir.util.VersionUtil;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

class HandlerTest {

    @Test
    void logStartupMessage() {
        LogCaptor logCaptor = LogCaptor.forClass(HandlerStream.class);

        new HandlerStream();

        assertTrue(logCaptor.getInfoLogs().contains("Validator is ready"));
    }

    @Test
    void handlerCanProcessEvent() throws IOException {
        VersionUtil versionUtil = new VersionUtil();
        String versionNumber = versionUtil.getVersion();
        String stepFunctionEvent = ResourceUtils.getResourceContent("examples/stepFunctionEvent.json");
        HandlerStream handlerStream = new HandlerStream();

        InputStream inputStream = new ByteArrayInputStream(stepFunctionEvent.getBytes());
        OutputStream outputStream = new ByteArrayOutputStream();

        handlerStream.handleRequest(inputStream, outputStream, mock(Context.class));

        JsonObject actualJsonResult = JsonParser.parseString(outputStream.toString()).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/stepFunctionResult.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }
}
