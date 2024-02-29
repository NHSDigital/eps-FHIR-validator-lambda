package software.nhs.FHIRValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;

public final class Utils {
    static Logger log = LogManager.getLogger(Utils.class);

    public static String getResourceContent(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            ByteArrayOutputStream result;
            try (InputStream inputStream = loader.getResourceAsStream(resource)) {
                result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];

                for (int length; (length = inputStream.read(buffer)) != -1;) {
                    result.write(buffer, 0, length);
                }
            }
            String rawData = result.toString("UTF-8");
            return rawData;

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new RuntimeException("error in getResourceContent", ex);
        }
    }

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
