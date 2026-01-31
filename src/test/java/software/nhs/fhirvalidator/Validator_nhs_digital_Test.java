package software.nhs.fhirvalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.VersionUtil;
import software.nhs.fhirvalidator.controller.ValidateController;
import software.nhs.fhirvalidator.util.ResourceUtils;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Validator_nhs_digital_Test {

    static ValidateController validateController;
    static FhirContext fhirContext;
    static String versionNumber;

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
                if (issue.getDiagnostics().trim().equals(diagnosticMessage)) {
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
        String manifest_file = "nhs_digital.manifest.json";
        validateController = new ValidateController(manifest_file);
        fhirContext = FhirContext.forR4();
        VersionUtil versionUtil = new VersionUtil();
        versionNumber = versionUtil.getVersion();
    }

    @Test
    void simpleBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        BundleEntryComponent bundleEntry = bundle.addEntry();
        bundleEntry.setResource(new Patient());
        String FHIRDocument = fhirContext.newJsonParser().encodeResourceToString(bundle);

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/validSimpleBundle.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);

        assertFalse(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.ERROR));
        assertFalse(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.WARNING));
    }

    @Test
    void validBundle() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/validBundle.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/validBundle.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);

        assertFalse(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.ERROR));
        assertTrue(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.WARNING));
    }

    @Test
    void invalidBundle() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/invalidBundle.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/invalidBundle.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);

        assertTrue(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.ERROR));
        assertTrue(issueListHasDiagnosticMessageAtSeverity(validatorResult.getIssue(), "Bundle entry missing fullUrl",
                OperationOutcome.IssueSeverity.ERROR));
        assertTrue(issueListHasDiagnosticMessageAtSeverity(validatorResult.getIssue(),
                "Unable to find a match for profile urn:uuid:56166769-c1c4-4d07-afa8-132b5dfca666 among choices: https://fhir.nhs.uk/StructureDefinition/NHSDigital-PractitionerRole-EPSLegal",
                OperationOutcome.IssueSeverity.ERROR));
        assertTrue(issueListHasDiagnosticMessageAtSeverity(validatorResult.getIssue(),
                "Except for transactions and batches, each entry in a Bundle must have a fullUrl which is the identity of the resource in the entry",
                OperationOutcome.IssueSeverity.ERROR));
    }

    @Test
    void validParameters() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/validParameters.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/validParameters.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void invalidParameters() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/invalidParameters.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/invalidParameters.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void validOperationOutcome() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/validOperationOutcome.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/successfulOperationOutcome.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void invalidOperationOutcome() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/invalidOperationOutcome.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/invalidOperationOutcome.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void searchSet() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/searchSet.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/searchSet.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void missingHeader() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/missingHeader.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/missingHeader.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void empty() {
        String FHIRDocument = "";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/empty.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void array() {
        String FHIRDocument = "[1,2,3]";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/array.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void null_java() {
        String FHIRDocument = null;

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/null_java.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void null_json() {
        String FHIRDocument = "null";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/null_json.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void number_json() {
        String FHIRDocument = "123";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/number_json.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void boolean_json() {
        String FHIRDocument = "true";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/boolean_json.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void bad_json() {
        String FHIRDocument = "{a:<>}}}";

        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/bad_json.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void psuUpdate() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/psu_update.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(fhirContext.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent(String.format("results/%s/psu_nhs_digital.json", versionNumber));
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);

        assertTrue(issueListHasSeverity(validatorResult.getIssue(), OperationOutcome.IssueSeverity.ERROR));
    }
}
