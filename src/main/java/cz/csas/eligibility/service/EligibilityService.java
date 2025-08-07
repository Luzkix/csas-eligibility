package cz.csas.eligibility.service;

import cz.csas.eligibility.model.GetEligibilityResponse;

/**
 * Service interface for Eligibility operations.
 */
public interface EligibilityService {
    /**
     * Function provides the result of evaluation of client's eligibility based on provided clientId.
     * @param clientId      the client identifier
     * @param correlationId the correlation id for tracing
     * @return non-null GetEligibilityResponse if evaluation was processed correctly or null in case of any error
     */
    GetEligibilityResponse evaluateEligibility(String clientId, String correlationId);
}
