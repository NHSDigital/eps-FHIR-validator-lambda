package software.nhs.fhirvalidator.util;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.OperationOutcome;

public class OperationOutcomeUtils {

    private OperationOutcomeUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static OperationOutcome createOperationOutcome(String diagnostics, String expression) {
        OperationOutcome.OperationOutcomeIssueComponent issue = createOperationOutcomeIssue(diagnostics, expression);
        List<OperationOutcome.OperationOutcomeIssueComponent> issues = Collections.singletonList(issue);
        return createOperationOutcome(issues);
    }

    public static OperationOutcome createOperationOutcome(
            List<OperationOutcome.OperationOutcomeIssueComponent> issues) {
        OperationOutcome operationOutcome = new OperationOutcome();
        issues.forEach(operationOutcome::addIssue);
        return operationOutcome;
    }

    public static OperationOutcome.OperationOutcomeIssueComponent createOperationOutcomeIssue(String diagnostics,
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

}
