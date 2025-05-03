package software.nhs.fhirvalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.uhn.fhir.context.FhirContext;
import software.nhs.fhirvalidator.controller.ValidateController;
import software.nhs.fhirvalidator.util.ResourceUtils;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Validator_uk_core_Test {

    static ValidateController validateController;
    static FhirContext fhirContext;

    Boolean issueListHasSeverity(List<OperationOutcomeIssueComponent> issueList, IssueSeverity severity) {
        for (OperationOutcomeIssueComponent issue : issueList) {
            if (issue.getSeverity().equals(severity)) {
                return true;
            }
        }
        return false;
    }

    Boolean issueListHasDiagnosticMessageAtSeverity(List<OperationOutcomeIssueComponent> issueList,
            String diagnosticMessage, IssueSeverity severity) {
        for (OperationOutcomeIssueComponent issue : issueList) {
            if (issue.getSeverity().equals(severity)) {
                if (issue.getDiagnostics().equals(diagnosticMessage)) {
                    return true;
                }
            }
        }
        return false;
    }

    @BeforeAll
    static void setup() {
        // Creating the HAPI validator takes several seconds. It's ok to reuse the same
        // validator across tests to speed up tests
        String manifest_file = "uk_core.manifest.json";
        validateController = new ValidateController(manifest_file);
        fhirContext = FhirContext.forR4();
    }

    @Test
    void psuUpdate() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/psu_update.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/psu_update.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);

        assertFalse(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.ERROR));
        //assertFalse(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.WARNING));
    }

    @Test
    void validBundle_nhsdigitalProfile() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/validBundle.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
  
        assertTrue(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.ERROR));
    }

    @Test
    void empty() {
        String FHIRDocument = "";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/empty.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void array() {
        String FHIRDocument = "[1,2,3]";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/array.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void null_java() {
        String FHIRDocument = null;

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/null_java.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void null_json() {
        String FHIRDocument = "null";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/null_json.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void number_json() {
        String FHIRDocument = "123";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/number_json.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void boolean_json() {
        String FHIRDocument = "true";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/boolean_json.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void bad_json() {
        String FHIRDocument = "{a:<>}}}";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/bad_json.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

}
