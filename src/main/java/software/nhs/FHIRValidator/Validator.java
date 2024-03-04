package software.nhs.FHIRValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.utilities.npm.NpmPackage;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import lombok.val;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.nhs.FHIRValidator.models.SimplifierPackage;
import software.amazon.lambda.powertools.logging.Logging;

/**
 * This class is a wrapper around the HAPI FhirValidator.
 * The FhirValidator is built using default settings and the available
 * implementation guides are loaded into it.
 */

public class Validator {
    private final FhirValidator validator;

    private final FhirContext ctx;
    private List<NpmPackage> npmPackages = new ArrayList<>();
    // private final CapabilityStatementApplier capabilityStatementApplier;
    Logger log = LogManager.getLogger(Validator.class);

    public Validator() {
        ctx = FhirContext.forR4();

        // Create a chain that will hold our modules
        ValidationSupportChain supportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(ctx),
                new CommonCodeSystemsTerminologyService(ctx),
                terminologyValidationSupport(ctx),
                new SnapshotGeneratingValidationSupport(ctx));

        SimplifierPackage[] packages = getPackages();

        try {
            for (SimplifierPackage individualPackage : packages) {
                String packagePath = String.format("classpath:package/%s-%s.tgz", individualPackage.packageName,
                        individualPackage.version);
                NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(ctx);
                npmPackageSupport.loadPackageFromClasspath(packagePath);
                supportChain.addValidationSupport(npmPackageSupport);
                try (InputStream is = ClasspathUtil.loadResourceAsStream(packagePath)) {
                    NpmPackage pkg = NpmPackage.fromPackage(is);
                    npmPackages.add(pkg);
                }
            }
        } catch (InternalErrorException | IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("error loading simplifier packages", ex);
        }

        generateSnapshots(supportChain);
        supportChain.fetchCodeSystem("http://snomed.info/sct");

        CachingValidationSupport validationSupport = new CachingValidationSupport(supportChain);

        // Create a validator using the FhirInstanceValidator module.
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(validationSupport);
        validator = ctx.newValidator().registerValidatorModule(validatorModule);
    }

    public OperationOutcome validate(String input) {

        try {
            IBaseResource inputResource = ctx.newJsonParser().parseResource(input);
            List<IBaseResource> resources = getResourcesToValidate(inputResource);

            List<OperationOutcome> operationOutcomeList = resources.stream()
                    .map(this::validateResource)
                    .collect(Collectors.toList());

            List<OperationOutcomeIssueComponent> operationOutcomeIssues = operationOutcomeList.stream()
                    .filter(Objects::nonNull)
                    .flatMap(operationOutcome -> operationOutcome.getIssue().stream())
                    .collect(Collectors.toList());

            return createOperationOutcome(operationOutcomeIssues);
        } catch (JsonSyntaxException | NullPointerException | IllegalArgumentException | InvalidRequestException
                | DataFormatException e) {
            log.error(e.toString());
            return createOperationOutcome(e.getMessage() != null ? e.getMessage() : "Invalid JSON", null);
        }
    }

    public OperationOutcome createOperationOutcome(String diagnostics, String expression) {
        OperationOutcome.OperationOutcomeIssueComponent issue = createOperationOutcomeIssue(diagnostics, expression);
        List<OperationOutcome.OperationOutcomeIssueComponent> issues = Collections.singletonList(issue);
        return createOperationOutcome(issues);
    }

    public OperationOutcome createOperationOutcome(List<OperationOutcome.OperationOutcomeIssueComponent> issues) {
        OperationOutcome operationOutcome = new OperationOutcome();
        issues.forEach(operationOutcome::addIssue);
        return operationOutcome;
    }

    public OperationOutcome.OperationOutcomeIssueComponent createOperationOutcomeIssue(String diagnostics,
            String expression) {
        OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setCode(OperationOutcome.IssueType.PROCESSING);
        issue.setDiagnostics(diagnostics);
        if (expression != null) {
            issue.addExpression(expression);
        }
        return issue;
    }

    private OperationOutcome validateResource(IBaseResource resource) {
        OperationOutcome result = (OperationOutcome) validator.validateWithResult(resource).toOperationOutcome();
        return result;
    }

    private List<IBaseResource> getResourcesToValidate(IBaseResource inputResource) {
        if (inputResource == null) {
            return new ArrayList<>();
        }

        if (inputResource instanceof Bundle && ((Bundle) inputResource).getType() == Bundle.BundleType.SEARCHSET) {
            List<IBaseResource> bundleResources = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : ((Bundle) inputResource).getEntry()) {
                bundleResources.add(entry.getResource());
            }

            if (bundleResources.stream().allMatch(resource -> resource.getResourceType() == ResourceType.Bundle)) {
                return bundleResources;
            }
        }

        return List.of(inputResource);
    }

    private ValidatorResponse toValidatorResponse(ValidationResult result) {
        return ValidatorResponse.builder()
                .isSuccessful(result.isSuccessful())
                .errorMessages(result.getMessages().stream()
                        .map(singleValidationMessage -> ValidatorErrorMessage.builder()
                                .severity(singleValidationMessage.getSeverity().getCode())
                                .msg(singleValidationMessage.getLocationString() + " - "
                                        + singleValidationMessage.getMessage())
                                .build())

                        .collect(Collectors.toList()))
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
        return !structureDefinition.hasSnapshot()
                && structureDefinition.getDerivation() == StructureDefinition.TypeDerivationRule.CONSTRAINT;
    }

    private StructureDefinition circularReferenceCheck(StructureDefinition structureDefinition,
            IValidationSupport supportChain) {
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
        StructureDefinition structureDefinition = (StructureDefinition) supportChain
                .fetchStructureDefinition(profile.toString());

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
                    IBaseResource theValueSet) {
                String valueSetUrl = CommonCodeSystemsTerminologyService.getValueSetUrl(fhirContext, theValueSet);

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
                        theValueSet);
            }
        };
    }

    private SimplifierPackage[] getPackages() {
        String manifestContent = Utils.getResourceContent("manifest.json");
        SimplifierPackage[] packages = new Gson().fromJson(manifestContent, SimplifierPackage[].class);
        return packages;
    }
}
