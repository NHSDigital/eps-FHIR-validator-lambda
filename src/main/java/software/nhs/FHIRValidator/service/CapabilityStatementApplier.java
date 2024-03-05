package software.nhs.FHIRValidator.service;

import software.nhs.FHIRValidator.util.FhirUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.utilities.npm.NpmPackage;

import java.io.IOException;
import java.util.List;

public class CapabilityStatementApplier {
    private final List<CapabilityStatement.CapabilityStatementRestResourceComponent> restResources;

    Logger log = LogManager.getLogger(CapabilityStatementApplier.class);

    public CapabilityStatementApplier(
            ImplementationGuideParser implementationGuideParser,
            List<NpmPackage> npmPackages) {

        this.restResources = npmPackages.stream()
                .flatMap(packageItem -> {
                    try {
                        return implementationGuideParser
                                .getResourcesOfType(packageItem, CapabilityStatement.class).stream();
                    } catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                        throw new RuntimeException("error in handleRequest", ex);
                    }
                })
                .flatMap(capabilityStatement -> capabilityStatement.getRest().stream())
                .flatMap(rest -> rest.getResource().stream())
                .toList();
    }

    public void applyCapabilityStatementProfiles(IBaseResource resource) {
        restResources.forEach(restResource -> applyRestResource(resource, restResource));
    }

    private void applyRestResource(
            IBaseResource resource,
            CapabilityStatement.CapabilityStatementRestResourceComponent restResource) {
        List<IBaseResource> matchingResources = FhirUtils.getResourcesOfType(resource, restResource.getType());
        if (restResource.hasProfile()) {
            FhirUtils.applyProfile(matchingResources, restResource.getProfileElement());
        }
    }
}
