package software.nhs.FHIRValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.uhn.fhir.context.FhirContext;
import software.nhs.FHIRValidator.controller.ValidateController;
import software.nhs.FHIRValidator.util.FhirUtils;
import software.nhs.FHIRValidator.util.ResourceUtils;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidatorTest {

    static ValidateController validateController;
    static FhirContext ctx;

    @BeforeAll
    static void setup() {
        // Creating the HAPI validator takes several seconds. It's ok to reuse the same
        // validator across tests to speed up tests
        validateController = new ValidateController();
        ctx = FhirContext.forR4();
    }

    @Test
    void validBundle() {
        String FHIRDocument = ResourceUtils.getResourceContent("examples/validBundle.json");
        OperationOutcome validatorResult = validateController.parseAndValidateResource(FHIRDocument);
        JsonObject actualJsonResult = JsonParser
                .parseString(ctx.newJsonParser().encodeResourceToString(validatorResult)).getAsJsonObject();

        String expectedResult = ResourceUtils.getResourceContent("results/validBundle.json");
        JsonObject expectedJsonResult = JsonParser.parseString(expectedResult).getAsJsonObject();

        assertEquals(expectedJsonResult, actualJsonResult);
    }

    /*
     * 
     * @Test
     * void invalidBundle() {
     * String FHIRDocument =
     * ResourceUtils.getResourceContent("examples/invalidBundle.json");
     * OperationOutcome validatorResult = validator.validate(FHIRDocument);
     * assertFalse((validatorResult.isSuccessful()));
     * ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
     * .msg("Bundle.entry[0] - Bundle entry missing fullUrl")
     * .severity("error")
     * .build();
     * ValidatorErrorMessage expectedMessage2 = ValidatorErrorMessage.builder()
     * .msg("Bundle.entry[0] - Except for transactions and batches, each entry in a Bundle must have a fullUrl which is the identity of the resource in the entry"
     * )
     * .severity("error")
     * .build();
     * List<ValidatorErrorMessage> errorMessages =
     * validatorResult.getErrorMessages();
     * assertEquals(errorMessages.size(), 2);
     * assertTrue(errorMessages.contains(expectedMessage1));
     * assertTrue(errorMessages.contains(expectedMessage2));
     * }
     * 
     * 
     * @Test
     * void warningBundle() {
     * String FHIRDocument =
     * ResourceUtils.getResourceContent("examples/warningBundle.json");
     * OperationOutcome validatorResult = validator.validate(FHIRDocument);
     * assertTrue((validatorResult.isSuccessful()));
     * ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
     * .sverity("warning")
     * .build();
     * List<ValidatorErrorMessage> errorMessages =
     * validatorResult.getErrorMessages();
     * assertEquals(errorMessages.size(), 1);
     * assertTrue(errorMessages.contains(expectedMessage1));
     * }
     * 
     * @Test
     * void mixedErrorWarningBundle() {
     * String FHIRDocument =
     * ResourceUtils.getResourceContent("examples/mixedErrorWarning.json");
     * OperationOutcome validatorResult = validator.validate(FHIRDocument);
     * assertFalse((validatorResult.isSuccessful()));
     * ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
     * .msg("Bundle.entry[0] - Bundle entry missing fullUrl")
     * .severity("error")
     * .build();
     * ValidatorErrorMessage expectedMessage2 = ValidatorErrorMessage.builder()
     * .msg("Bundle.entry[0] - Except for transactions and batches, each entry in a Bundle must have a fullUrl which is the identity of the resource in the entry"
     * )
     * .severity("error")
     * .build();
     * ValidatorErrorMessage expectedMessage3 = ValidatorErrorMessage.builder()
     * .severity("warning")
     * .build();
     * ValidatorErrorMessage expectedMessage4 = ValidatorErrorMessage.builder()
     * .severity("warning")
     * .build();
     * List<ValidatorErrorMessage> errorMessages =
     * validatorResult.getErrorMessages();
     * assertEquals(errorMessages.size(), 4);
     * assertTrue(errorMessages.contains(expectedMessage1));
     * assertTrue(errorMessages.contains(expectedMessage2));
     * assertTrue(errorMessages.contains(expectedMessage3));
     * assertTrue(errorMessages.contains(expectedMessage4));
     * }
     * 
     * @Test
     * void validParameters() {
     * String FHIRDocument =
     * ResourceUtils.getResourceContent("examples/validParameters.json");
     * OperationOutcome validatorResult = validator.validate(FHIRDocument);
     * assertTrue((validatorResult.isSuccessful()));
     * List<ValidatorErrorMessage> errorMessages =
     * validatorResult.getErrorMessages();
     * assertEquals(errorMessages.size(), 0);
     * }
     * 
     * @Test
     * void invalidParameters() {
     * String FHIRDocument =
     * ResourceUtils.getResourceContent("examples/invalidParameters.json");
     * OperationOutcome validatorResult = validator.validate(FHIRDocument);
     * assertFalse((validatorResult.isSuccessful()));
     * List<ValidatorErrorMessage> errorMessages =
     * validatorResult.getErrorMessages();
     * ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
     * .msg("Parameters.parameter[0] - Rule inv-1: 'A parameter must have one and only one of (value, resource, part)' Failed"
     * )
     * .severity("error")
     * .build();
     * assertEquals(errorMessages.size(), 1);
     * assertTrue(errorMessages.contains(expectedMessage1));
     * }
     * 
     * @Test
     * void validOperationOutcome() {
     * String FHIRDocument =
     * ResourceUtils.getResourceContent("examples/validOperationOutcome.json");
     * OperationOutcome validatorResult = validator.validate(FHIRDocument);
     * assertTrue((validatorResult.isSuccessful()));
     * List<ValidatorErrorMessage> errorMessages =
     * validatorResult.getErrorMessages();
     * assertEquals(errorMessages.size(), 0);
     * }
     * 
     * @Test
     * void invalidOperationOutcome() {
     * String FHIRDocument =
     * ResourceUtils.getResourceContent("examples/invalidOperationOutcome.json");
     * OperationOutcome validatorResult = validator.validate(FHIRDocument);
     * assertFalse((validatorResult.isSuccessful()));
     * List<ValidatorErrorMessage> errorMessages =
     * validatorResult.getErrorMessages();
     * ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
     * .msg("OperationOutcome - OperationOutcome.issue: minimum required = 1, but only found 0 (from http://hl7.org/fhir/StructureDefinition/OperationOutcome|4.0.1)"
     * )
     * .severity("error")
     * .build();
     * assertEquals(errorMessages.size(), 1);
     * assertTrue(errorMessages.contains(expectedMessage1));
     * }
     * 
     * @Test
     * void empty() {
     * String resourceText = "";
     * assertEquals(validator.validate(resourceText),
     * INVALID_JSON_VALIDATOR_RESPONSE);
     * }
     * 
     * @Test
     * void array() {
     * String resourceText = "[1,2,3]";
     * assertEquals(validator.validate(resourceText),
     * INVALID_JSON_VALIDATOR_RESPONSE);
     * }
     * 
     * @Test
     * void null_json() {
     * String resourceText = "null";
     * assertEquals(validator.validate(resourceText),
     * INVALID_JSON_VALIDATOR_RESPONSE);
     * 
     * }
     * 
     * @Test
     * void null_java() {
     * String resourceText = null;
     * assertEquals(validator.validate(resourceText),
     * INVALID_JSON_VALIDATOR_RESPONSE);
     * }
     * 
     * @Test
     * void number_json() {
     * String resourceText = "123";
     * assertEquals(validator.validate(resourceText),
     * INVALID_JSON_VALIDATOR_RESPONSE);
     * }
     * 
     * @Test
     * void boolean_json() {
     * String resourceText = "true";
     * assertEquals(validator.validate(resourceText),
     * INVALID_JSON_VALIDATOR_RESPONSE);
     * }
     * 
     * @Test
     * void bad_json() {
     * String resourceText = "{a:<>}}}";
     * assertEquals(validator.validate(resourceText),
     * INVALID_JSON_VALIDATOR_RESPONSE);
     * }
     */

}
