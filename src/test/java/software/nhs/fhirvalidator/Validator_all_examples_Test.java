package software.nhs.fhirvalidator;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.opentest4j.AssertionFailedError;

import software.nhs.fhirvalidator.controller.ValidateController;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

class Validator_all_examples_Test {

    private final ClassLoader loader = Thread.currentThread().getContextClassLoader();
    private static ValidateController testValidateController;

    static Stream<File> getExampleFhirFiles() {
        try {
            URL resource = Thread.currentThread().getContextClassLoader().getResource("examples/full_examples");
            if (resource == null) {
                throw new IllegalStateException("Could not find examples directory");
            }
            File dir = new File(resource.getFile());
            return Files.walk(dir.toPath())
                .filter(Files::isRegularFile)
                .map(path -> path.toFile())
                .sorted();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load example files", e);
        }
    }

    @BeforeAll
    static void setup() {
        String manifest_file = "nhs_digital.manifest.json";
        testValidateController = new ValidateController(manifest_file);
    }

    @DisplayName("Test all valid example files")
    @ParameterizedTest(name = "Validating file: {0}")
    @MethodSource("getExampleFhirFiles")
    void testFhirExamples(File exampleFile) throws IOException {
        List<String> lines = Files.readAllLines(exampleFile.toPath());
        String fileContent = String.join(" ", lines);

        OperationOutcome actualResult = testValidateController.parseAndValidateResource(fileContent);

        for (OperationOutcome.OperationOutcomeIssueComponent issue : actualResult.getIssue()) {
            if (issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR) {
                throw new AssertionFailedError("Error found checking file " + exampleFile.getAbsolutePath() +
                        ". Error: " + issue.getDiagnostics());
            }
        }
    }
}
