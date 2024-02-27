package software.nhs.FHIRValidator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;


/**
 * This class is a wrapper around the HAPI FhirValidator.
 * The FhirValidator is built using default settings and the available implementation guides are loaded into it.
 */
@Slf4j
public class Validator {
    private static final Gson GSON = new Gson();
    public static final String DEFAULT_IMPLEMENTATION_GUIDES_FOLDER = "implementationGuides";
    public static final String FHIR_R4 = "4.0.1";
    public static final String FHIR_STU3 = "3.0.1";

    private final FhirValidator validator;

    private final FhirContext ctx;


    public Validator() {

        // To learn more about the different ways to configure FhirInstanceValidator see: https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html
        ctx = FhirContext.forR4();

        // Create a chain that will hold our modules
        ValidationSupportChain supportChain = new ValidationSupportChain();

        // DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
        // even if you are using custom profiles, since those profiles will derive from the base
        // definitions.
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(ctx);
        supportChain.addValidationSupport(defaultSupport);

        // This module supplies several code systems that are commonly used in validation
        supportChain.addValidationSupport(new CommonCodeSystemsTerminologyService(ctx));

        // This module implements terminology services for in-memory code validation
        supportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(ctx));

        NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(ctx);
        try {
            npmPackageSupport.loadPackageFromClasspath("classpath:package/fhir.r4.ukcore.stu3.currentbuild-0.0.3-pre-release.tgz");
            npmPackageSupport.loadPackageFromClasspath("classpath:package/uk.nhsdigital.r4.test-2.8.17-prerelease.tgz");
            npmPackageSupport.loadPackageFromClasspath("classpath:package/uk.nhsdigital.medicines.r4.test-2.8.3-prerelease.tgz");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        supportChain.addValidationSupport(npmPackageSupport);

        // Create a validator using the FhirInstanceValidator module.
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(supportChain);
        validator = ctx.newValidator().registerValidatorModule(validatorModule);
    }

    public ValidatorResponse validate(String resourceAsJsonText) {
        try {
            ValidationResult result = validator.validateWithResult(resourceAsJsonText);
            return toValidatorResponse(result);
        } catch (JsonSyntaxException | NullPointerException | IllegalArgumentException | InvalidRequestException e) {
            return ValidatorResponse.builder()
                .isSuccessful(false)
                .errorMessages(ImmutableList.of(ValidatorErrorMessage.builder()
                    .msg("Invalid JSON")
                    .severity("error")
                    .build()))
                .build();
        }
    }

    private ValidatorResponse toValidatorResponse(ValidationResult result) {
        return ValidatorResponse.builder()
            .isSuccessful(result.isSuccessful())
            .errorMessages(result.getMessages().stream()
                .map(singleValidationMessage -> ValidatorErrorMessage.builder()
                    .severity(singleValidationMessage.getSeverity().getCode())
                    .msg(singleValidationMessage.getLocationString() + " - " + singleValidationMessage.getMessage())
                    .build())
                    
                .collect(Collectors.toList())
            )
            .build();
    }

    //private List<NpmPackage>() {
    //    val inputStream = ClassPathResource("manifest.json").inputStream;
    //    val packages = objectMapper.readValue(inputStream, Array<SimplifierPackage>::class.java);
    //    return Arrays.stream(packages)
    //        .map { "${it.packageName}-${it.version}.tgz" }
    //        .map { ClassPathResource(it).inputStream }
    //        .map { NpmPackage.fromPackage(it) }
    //        .toList();
    //}
}
