package software.nhs.FHIRValidator;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
class ValidatorResponse {
    private boolean isSuccessful;
    private List<ValidatorErrorMessage> errorMessages;
}

@Builder
@Value
class ValidatorErrorMessage {
    private String severity;
    private String msg;
}
