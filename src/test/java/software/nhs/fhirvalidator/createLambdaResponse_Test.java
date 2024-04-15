package software.nhs.fhirvalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeAll;

import software.nhs.fhirvalidator.controller.ValidateController;
import software.nhs.fhirvalidator.util.OperationOutcomeUtils;
import software.nhs.fhirvalidator.util.ResourceUtils;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.uhn.fhir.context.FhirContext;

public class createLambdaResponse_Test {
    static ValidateController validateController;
    static FhirContext fhirContext;
    static OperationOutcome.OperationOutcomeIssueComponent informationIssue;
    static OperationOutcome.OperationOutcomeIssueComponent warningIssue;
    static OperationOutcome.OperationOutcomeIssueComponent errorIssue;
    
    @BeforeAll
    static void setup() {
        // Creating the HAPI validator takes several seconds. It's ok to reuse the same
        // validator across tests to speed up tests
        String manifest_file = "uk_core.manifest.json";
        validateController = new ValidateController(manifest_file);
        fhirContext = FhirContext.forR4();

        informationIssue = new OperationOutcome.OperationOutcomeIssueComponent();
        informationIssue.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
        informationIssue.setCode(OperationOutcome.IssueType.INFORMATIONAL);
        informationIssue.setDiagnostics("This is information");

        warningIssue = new OperationOutcome.OperationOutcomeIssueComponent();
        warningIssue.setSeverity(OperationOutcome.IssueSeverity.WARNING);
        warningIssue.setCode(OperationOutcome.IssueType.INFORMATIONAL);
        warningIssue.setDiagnostics("This is a warning");

        errorIssue = new OperationOutcome.OperationOutcomeIssueComponent();
        errorIssue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        errorIssue.setCode(OperationOutcome.IssueType.EXCEPTION);
        errorIssue.setDiagnostics("This is an error");
    }
    
    @Test
    void SuccessfulResponseWhenJustInformation() {
        List<OperationOutcome.OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcome.OperationOutcomeIssueComponent>(); 
        issues.add(informationIssue);
        OperationOutcome operationOutcome = OperationOutcomeUtils.createOperationOutcome(issues);
        String actualResult = validateController.createLambdaResponse(operationOutcome);
        JsonObject actualJsonResult = JsonParser.parseString(actualResult).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/lambdaResponse_success_information.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();
        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void SuccessfulResponseWhenJustWarning() {
        List<OperationOutcome.OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcome.OperationOutcomeIssueComponent>(); 
        issues.add(warningIssue);
        OperationOutcome operationOutcome = OperationOutcomeUtils.createOperationOutcome(issues);
        String actualResult = validateController.createLambdaResponse(operationOutcome);
        JsonObject actualJsonResult = JsonParser.parseString(actualResult).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/lambdaResponse_success_warning.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();
        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void SuccessfulResponseWhenMixedInformationWarning() {
        List<OperationOutcome.OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcome.OperationOutcomeIssueComponent>(); 
        issues.add(informationIssue);
        issues.add(warningIssue);
        OperationOutcome operationOutcome = OperationOutcomeUtils.createOperationOutcome(issues);
        String actualResult = validateController.createLambdaResponse(operationOutcome);
        JsonObject actualJsonResult = JsonParser.parseString(actualResult).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/lambdaResponse_success_mixed.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();
        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void FailureResponseWhenJustError() {
        List<OperationOutcome.OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcome.OperationOutcomeIssueComponent>(); 
        issues.add(errorIssue);
        OperationOutcome operationOutcome = OperationOutcomeUtils.createOperationOutcome(issues);
        String actualResult = validateController.createLambdaResponse(operationOutcome);
        JsonObject actualJsonResult = JsonParser.parseString(actualResult).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/lambdaResponse_failure_error.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();
        assertEquals(expectedJsonResult, actualJsonResult);
    }

    @Test
    void FailureResponseWhenMixedIncludingError() {
        List<OperationOutcome.OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcome.OperationOutcomeIssueComponent>(); 
        issues.add(informationIssue);
        issues.add(errorIssue);
        OperationOutcome operationOutcome = OperationOutcomeUtils.createOperationOutcome(issues);
        String actualResult = validateController.createLambdaResponse(operationOutcome);
        JsonObject actualJsonResult = JsonParser.parseString(actualResult).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/lambdaResponse_failure_mixed.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();
        assertEquals(expectedJsonResult, actualJsonResult);
    }
}
