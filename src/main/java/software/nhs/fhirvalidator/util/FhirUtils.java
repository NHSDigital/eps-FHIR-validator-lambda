package software.nhs.fhirvalidator.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;

public final class FhirUtils {
    static Logger log = LogManager.getLogger(FhirUtils.class);

    public static List<IBaseResource> getResourcesOfType(IBaseResource resource, String resourceType) {
        List<IBaseResource> matchingResources = new ArrayList<>();

        if (resource.fhirType().equals(resourceType)) {
            matchingResources.add(resource);
        }

        if (resource instanceof Bundle) {
            Bundle bundle = (Bundle) resource;
            bundle.getEntry().stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(entryResource -> entryResource.fhirType().equals(resourceType))
                    .forEach(matchingResources::add);
        }

        return matchingResources;
    }

    public static void applyProfile(List<IBaseResource> resources, IPrimitiveType<String> profile) {
        resources.forEach(resource -> {
            resource.getMeta().getProfile().clear();
            resource.getMeta().addProfile(profile.getValue());
        });
    }
}
