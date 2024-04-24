package software.nhs.fhirvalidator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.altindag.log.LogCaptor;
import software.nhs.fhirvalidator.handler.HandleRequest;

import org.junit.jupiter.api.Test;

class HandlerTest {

    @Test
    void logInfoAndWarnMessages() {
        LogCaptor logCaptor = LogCaptor.forClass(HandleRequest.class);

        new HandleRequest();

        assertTrue(logCaptor.getInfoLogs().contains("Validator is ready"));

    }
}
