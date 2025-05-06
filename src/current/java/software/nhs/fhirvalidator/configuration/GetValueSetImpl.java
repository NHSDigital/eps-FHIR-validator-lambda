package software.nhs.fhirvalidator.configuration;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import software.nhs.fhirvalidator.configuration.GetValueSetInterface;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;

public class GetValueSetImpl implements GetValueSetInterface {
    public String getValueSetUrl(FhirContext fhirContext, IBaseResource theValueSet) {
        return CommonCodeSystemsTerminologyService.getValueSetUrl(fhirContext, theValueSet);
    }
}
