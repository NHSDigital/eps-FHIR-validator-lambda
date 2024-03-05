package software.nhs.fhirvalidator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.altindag.log.LogCaptor;
import software.nhs.fhirvalidator.handler.HandlerStream;

import org.junit.jupiter.api.Test;

public class HandlerTest {

    @Test
    public void logInfoAndWarnMessages() {
        LogCaptor logCaptor = LogCaptor.forClass(HandlerStream.class);

        new HandlerStream();

        assertTrue(logCaptor.getInfoLogs().contains("Validator is ready"));

    }
}
