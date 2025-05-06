package software.nhs.fhirvalidator.configuration;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface GetValueSetInterface {
    String getValueSetUrl(FhirContext fhirContext, IBaseResource theValueSet);
}
