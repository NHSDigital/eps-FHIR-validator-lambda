package software.nhs.FHIRValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidatorTest {

    public static final ValidatorResponse INVALID_JSON_VALIDATOR_RESPONSE = ValidatorResponse.builder()
            .isSuccessful(false)
            .errorMessages(ImmutableList.of(ValidatorErrorMessage.builder()
                    .msg("Invalid JSON")
                    .severity("error")
                    .build()))
            .build();
    static Validator validator;

    @BeforeAll
    static void setup() {
        // Creating the HAPI validator takes several seconds. It's ok to reuse the same
        // validator across tests to speed up tests
        validator = new Validator();
    }

    @Test
    void validBundle() {
        String FHIRDocument = Utils.getResourceContent("examples/validBundle.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertTrue((validatorResult.isSuccessful()));
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        assertEquals(errorMessages.size(), 0);
    }

    @Test
    void invalidBundle() {
        String FHIRDocument = Utils.getResourceContent("examples/invalidBundle.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertFalse((validatorResult.isSuccessful()));
        ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
                .msg("Bundle.entry[0] - Bundle entry missing fullUrl")
                .severity("error")
                .build();
        ValidatorErrorMessage expectedMessage2 = ValidatorErrorMessage.builder()
                .msg("Bundle.entry[0] - Except for transactions and batches, each entry in a Bundle must have a fullUrl which is the identity of the resource in the entry")
                .severity("error")
                .build();
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        assertEquals(errorMessages.size(), 2);
        assertTrue(errorMessages.contains(expectedMessage1));
        assertTrue(errorMessages.contains(expectedMessage2));
    }

    @Test
    void warningBundle() {
        String FHIRDocument = Utils.getResourceContent("examples/warningBundle.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertTrue((validatorResult.isSuccessful()));
        ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
                .msg("Bundle.entry[4].resource/*PractitionerRole/null*/.code[0].coding[0] - Unknown code in fragment CodeSystem 'https://fhir.nhs.uk/CodeSystem/NHSDigital-SDS-JobRoleCode#S8000:G8000:R8000' for 'https://fhir.nhs.uk/CodeSystem/NHSDigital-SDS-JobRoleCode#S8000:G8000:R8000'")
                .severity("warning")
                .build();
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        assertEquals(errorMessages.size(), 1);
        assertTrue(errorMessages.contains(expectedMessage1));
    }

    @Test
    void mixedErrorWarningBundle() {
        String FHIRDocument = Utils.getResourceContent("examples/mixedErrorWarning.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertFalse((validatorResult.isSuccessful()));
        ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
                .msg("Bundle.entry[0] - Bundle entry missing fullUrl")
                .severity("error")
                .build();
        ValidatorErrorMessage expectedMessage2 = ValidatorErrorMessage.builder()
                .msg("Bundle.entry[0] - Except for transactions and batches, each entry in a Bundle must have a fullUrl which is the identity of the resource in the entry")
                .severity("error")
                .build();
        ValidatorErrorMessage expectedMessage3 = ValidatorErrorMessage.builder()
                .msg("Bundle.entry[4].resource/*PractitionerRole/null*/.code[0].coding[0] - Unknown code in fragment CodeSystem 'https://fhir.nhs.uk/CodeSystem/NHSDigital-SDS-JobRoleCode#S8000:G8000:R8000' for 'https://fhir.nhs.uk/CodeSystem/NHSDigital-SDS-JobRoleCode#S8000:G8000:R8000'")
                .severity("warning")
                .build();
        ValidatorErrorMessage expectedMessage4 = ValidatorErrorMessage.builder()
                .msg("Bundle.entry[4].resource/*PractitionerRole/null*/.code[0].coding[0].display - value should not start or finish with whitespace 'Clinical Practitioner Access Role '")
                .severity("warning")
                .build();
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        assertEquals(errorMessages.size(), 4);
        assertTrue(errorMessages.contains(expectedMessage1));
        assertTrue(errorMessages.contains(expectedMessage2));
        assertTrue(errorMessages.contains(expectedMessage3));
        assertTrue(errorMessages.contains(expectedMessage4));
    }

    @Test
    void validParameters() {
        String FHIRDocument = Utils.getResourceContent("examples/validParameters.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertTrue((validatorResult.isSuccessful()));
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        assertEquals(errorMessages.size(), 0);
    }

    @Test
    void invalidParameters() {
        String FHIRDocument = Utils.getResourceContent("examples/invalidParameters.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertFalse((validatorResult.isSuccessful()));
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
                .msg("Parameters.parameter[0] - Rule inv-1: 'A parameter must have one and only one of (value, resource, part)' Failed")
                .severity("error")
                .build();
        assertEquals(errorMessages.size(), 1);
        assertTrue(errorMessages.contains(expectedMessage1));
    }

    @Test
    void validOperationOutcome() {
        String FHIRDocument = Utils.getResourceContent("examples/validOperationOutcome.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertTrue((validatorResult.isSuccessful()));
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        assertEquals(errorMessages.size(), 0);
    }

    @Test
    void invalidOperationOutcome() {
        String FHIRDocument = Utils.getResourceContent("examples/invalidOperationOutcome.json");
        ValidatorResponse validatorResult = validator.validate(FHIRDocument);
        assertFalse((validatorResult.isSuccessful()));
        List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
        ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
                .msg("OperationOutcome - OperationOutcome.issue: minimum required = 1, but only found 0 (from http://hl7.org/fhir/StructureDefinition/OperationOutcome|4.0.1)")
                .severity("error")
                .build();
        assertEquals(errorMessages.size(), 1);
        assertTrue(errorMessages.contains(expectedMessage1));
    }

    @Test
    void empty() {
        String resourceText = "";
        assertEquals(validator.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE);
    }

    @Test
    void array() {
        String resourceText = "[1,2,3]";
        assertEquals(validator.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE);
    }

    @Test
    void null_json() {
        String resourceText = "null";
        assertEquals(validator.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE);

    }

    @Test
    void null_java() {
        String resourceText = null;
        assertEquals(validator.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE);
    }

    @Test
    void number_json() {
        String resourceText = "123";
        assertEquals(validator.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE);
    }

    @Test
    void boolean_json() {
        String resourceText = "true";
        assertEquals(validator.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE);
    }

    @Test
    void bad_json() {
        String resourceText = "{a:<>}}}";
        assertEquals(validator.validate(resourceText), INVALID_JSON_VALIDATOR_RESPONSE);
    }
}
