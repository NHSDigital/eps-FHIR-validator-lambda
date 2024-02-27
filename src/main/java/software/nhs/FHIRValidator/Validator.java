package software.nhs.FHIRValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.utilities.npm.NpmPackage;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.nhs.FHIRValidator.models.SimplifierPackage;
import software.amazon.lambda.powertools.logging.Logging;


/**
 * This class is a wrapper around the HAPI FhirValidator.
 * The FhirValidator is built using default settings and the available implementation guides are loaded into it.
 */

public class Validator {
    private static final Gson GSON = new Gson();
    public static final String DEFAULT_IMPLEMENTATION_GUIDES_FOLDER = "implementationGuides";
    public static final String FHIR_R4 = "4.0.1";
    public static final String FHIR_STU3 = "3.0.1";

    private final FhirValidator validator;

    private final FhirContext ctx;
    Logger log = LogManager.getLogger(Validator.class);

    public Validator() {
        ctx = FhirContext.forR4();

        // Create a chain that will hold our modules
        ValidationSupportChain supportChain = new ValidationSupportChain(
            new DefaultProfileValidationSupport(ctx),
            new CommonCodeSystemsTerminologyService(ctx),
            terminologyValidationSupport(ctx),
            new SnapshotGeneratingValidationSupport(ctx)
        );

        NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(ctx);
        SimplifierPackage[] packages = getPackages();
        try {
            for (SimplifierPackage individualPackage : packages) {
                String packagePath = String.format("classpath:package/%s-%s.tgz", individualPackage.packageName, individualPackage.version);
                npmPackageSupport.loadPackageFromClasspath(packagePath);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        supportChain.addValidationSupport(npmPackageSupport);
        generateSnapshots(supportChain);
        supportChain.fetchCodeSystem("http://snomed.info/sct");

        // Create a validator using the FhirInstanceValidator module.
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(supportChain);
        validator = ctx.newValidator().registerValidatorModule(validatorModule);
    }

    @Logging
    public ValidatorResponse validate(String resourceAsJsonText) {
        try {
            ValidationResult result = validator.validateWithResult(resourceAsJsonText);
            return toValidatorResponse(result);
        } catch (JsonSyntaxException | NullPointerException | IllegalArgumentException | InvalidRequestException e) {
            log.error(e.toString());
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

    private void generateSnapshots(IValidationSupport supportChain) {
    List<StructureDefinition> structureDefinitions = supportChain.fetchAllStructureDefinitions();
    if (structureDefinitions == null) {
        return;
    }
    
    ValidationSupportContext context = new ValidationSupportContext(supportChain);

    structureDefinitions.stream()
            .filter(this::shouldGenerateSnapshot)
            .forEach(it -> {
                try {
                    circularReferenceCheck(it, supportChain);
                } catch (Exception e) {
                    log.error("Failed to generate snapshot for " + it, e);
                }
            });

    structureDefinitions.stream()
            .filter(this::shouldGenerateSnapshot)
            .forEach(it -> {
                try {
                    supportChain.generateSnapshot(context, it, it.getUrl(), "https://fhir.nhs.uk/R4", it.getName());
                } catch (Exception e) {
                    log.error("Failed to generate snapshot for " + it, e);
                }
            });
    }

    private boolean shouldGenerateSnapshot(StructureDefinition structureDefinition) {
        return !structureDefinition.hasSnapshot() && structureDefinition.getDerivation() == StructureDefinition.TypeDerivationRule.CONSTRAINT;
    }

    private StructureDefinition circularReferenceCheck(StructureDefinition structureDefinition, IValidationSupport supportChain) {
        if (structureDefinition.hasSnapshot()) {
            log.error(structureDefinition.getUrl() + " has snapshot!!");
        }

        for (ElementDefinition element : structureDefinition.getDifferential().getElement()) {
            if ((element.getId().endsWith(".partOf") ||
                element.getId().endsWith(".basedOn") ||
                element.getId().endsWith(".replaces") ||
                element.getId().contains("Condition.stage.assessment") ||
                element.getId().contains("Observation.derivedFrom") ||
                element.getId().contains("Observation.hasMember") ||
                element.getId().contains("CareTeam.encounter") ||
                element.getId().contains("CareTeam.reasonReference") ||
                element.getId().contains("ServiceRequest.encounter") ||
                element.getId().contains("ServiceRequest.reasonReference") ||
                element.getId().contains("EpisodeOfCare.diagnosis.condition") ||
                element.getId().contains("Encounter.diagnosis.condition") ||
                element.getId().contains("Encounter.reasonReference") ||
                element.getId().contains("Encounter.appointment")) && element.hasType()) {
                
                log.warn(structureDefinition.getUrl() + " has circular references (" + element.getId() + ")");
                
                for (ElementDefinition.TypeRefComponent typeRef : element.getType()) {
                    if (typeRef.hasTargetProfile()) {
                        for (CanonicalType targetProfile : typeRef.getTargetProfile()) {
                            typeRef.setTargetProfile((List<CanonicalType>) getBase(targetProfile, supportChain));
                        }
                    }
                }
            }
        }
        return structureDefinition;
    }

    private CanonicalType getBase(CanonicalType profile, IValidationSupport supportChain) {
        StructureDefinition structureDefinition = (StructureDefinition) supportChain.fetchStructureDefinition(profile.toString());
        
        if (structureDefinition != null && structureDefinition.hasBaseDefinition()) {
            String baseProfile = structureDefinition.getBaseDefinition();
            CanonicalType canonicalBaseProfile = new CanonicalType(baseProfile);
            if (baseProfile.contains(".uk")) {
                canonicalBaseProfile = getBase(canonicalBaseProfile, supportChain);
            }
            return canonicalBaseProfile;
        }
        return null;
    }

    private InMemoryTerminologyServerValidationSupport terminologyValidationSupport(FhirContext fhirContext) {
    return new InMemoryTerminologyServerValidationSupport(fhirContext) {
        @Override
        public IValidationSupport.CodeValidationResult validateCodeInValueSet(
            ValidationSupportContext theValidationSupportContext,
            ConceptValidationOptions theOptions,
            String theCodeSystem,
            String theCode,
            String theDisplay,
            IBaseResource theValueSet
        ) {
            String valueSetUrl = CommonCodeSystemsTerminologyService.getValueSetUrl(theValueSet);

            if ("https://fhir.nhs.uk/ValueSet/NHSDigital-MedicationRequest-Code".equals(valueSetUrl)
                || "https://fhir.nhs.uk/ValueSet/NHSDigital-MedicationDispense-Code".equals(valueSetUrl)
                || "https://fhir.hl7.org.uk/ValueSet/UKCore-MedicationCode".equals(valueSetUrl)) {
                return new IValidationSupport.CodeValidationResult()
                    .setSeverity(IValidationSupport.IssueSeverity.WARNING)
                    .setMessage("Unable to validate medication codes");
            }

            return super.validateCodeInValueSet(
                theValidationSupportContext,
                theOptions,
                theCodeSystem,
                theCode,
                theDisplay,
                theValueSet
            );
        }};
    }
    
    private SimplifierPackage[] getPackages() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream("manifest.json");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            String rawInput=result.toString("UTF-8");
            SimplifierPackage[] packages = new Gson().fromJson(rawInput, SimplifierPackage[].class);
            return packages;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
