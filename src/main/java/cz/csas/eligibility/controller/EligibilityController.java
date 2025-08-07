package cz.csas.eligibility.controller;

import cz.csas.eligibility.api_ui.ApplicationServerApi;
import cz.csas.eligibility.model.GetEligibilityResponse;
import cz.csas.eligibility.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class EligibilityController implements ApplicationServerApi {

    private final EligibilityService eligibilityService;

    @Override
    public ResponseEntity<GetEligibilityResponse> apiV1EligibilityGet(String clientId, String correlationId) {
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(clientId, correlationId);

        if (response != null) {
            return ResponseEntity
                    .status(200)
                    .header("correlation-id", correlationId)
                    .body(response);
        } else return ResponseEntity
                .status(400)
                .header("correlation-id", correlationId)
                .body(null);
    }
}
