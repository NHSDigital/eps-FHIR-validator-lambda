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

import software.nhs.FHIRValidator.Validator;
import software.nhs.FHIRValidator.ValidatorErrorMessage;
import software.nhs.FHIRValidator.ValidatorResponse;
import software.nhs.FHIRValidator.models.SimplifierPackage;

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
    static Validator validatorStu3;

    @BeforeAll
    static void setup() {
        // Creating the HAPI validator takes several seconds. It's ok to reuse the same validator across tests to speed up tests
        validator = new Validator();
        //validatorStu3 = new Validator(Validator.FHIR_STU3, "testImplementationGuides-stu3");
    }

    @Test
    void validFHIR() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream("examples/validFHIR.json");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            String rawInput=result.toString("UTF-8");
            ValidatorResponse validatorResult = validator.validate(rawInput);
            assertTrue((validatorResult.isSuccessful()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    void invalidFHIR() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream("examples/invalidFHIR.json");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            String rawInput=result.toString("UTF-8");
            ValidatorResponse validatorResult = validator.validate(rawInput);
            assertFalse((validatorResult.isSuccessful()));
            ValidatorErrorMessage expectedMessage1 = ValidatorErrorMessage.builder()
                    .msg("Bundle.entry[0] - Bundle entry missing fullUrl")
                    .severity("error")
                    .build();
            ValidatorErrorMessage expectedMessage2 = ValidatorErrorMessage.builder()
                    .msg("Bundle.entry[0] - Except for transactions and batches, each entry in a Bundle must have a fullUrl which is the identity of the resource in the entry  ")
                    .severity("error")
                    .build();                    
            List<ValidatorErrorMessage> errorMessages = validatorResult.getErrorMessages();
            assertTrue(errorMessages.contains(expectedMessage1));
            assertTrue(errorMessages.contains(expectedMessage2));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
