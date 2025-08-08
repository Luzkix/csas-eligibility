package cz.csas.eligibility.service.impl;

import cz.csas.eligibility.entity.Eligibility;
import cz.csas.eligibility.exceptions.EligibilityApiException;
import cz.csas.eligibility.model.Account;
import cz.csas.eligibility.model.GetClientDetailResponse;
import cz.csas.eligibility.model.GetEligibilityResponse;
import cz.csas.eligibility.repository.EligibilityRepository;
import cz.csas.eligibility.service.ApiServiceAccounts;
import cz.csas.eligibility.service.ApiServiceClients;
import cz.csas.eligibility.service.EligibilityService;
import cz.csas.eligibility.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of EligibilityService methods
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EligibilityServiceImpl implements EligibilityService {

    private final ApiServiceAccounts apiServiceAccounts;
    private final ApiServiceClients apiServiceClients;
    private final EligibilityRepository eligibilityRepository;

    @Override
    public GetEligibilityResponse evaluateEligibility(String clientId, String correlationId) {
        try {
            GetEligibilityResponse eligibilityResponse = new GetEligibilityResponse();

            List<Account> accounts = apiServiceAccounts.getClientAccounts(clientId, correlationId);
            GetClientDetailResponse clientDetail = apiServiceClients.getClientDetail(clientId, correlationId);
            boolean clientIsAdult = DateUtils.isAdult(clientDetail.getBirthDate());

            if (!accounts.isEmpty() && clientIsAdult) {
                eligibilityResponse.setEligible(true);
                saveResult(clientId, correlationId, Eligibility.EligibilityResultEnum.ELIGIBLE);
                return eligibilityResponse;
            } else {
                List<GetEligibilityResponse.ReasonsEnum> reasons = new ArrayList<>();

                if (accounts.isEmpty()) reasons.add(GetEligibilityResponse.ReasonsEnum.NO_ACCOUNT);
                if (!clientIsAdult) reasons.add(GetEligibilityResponse.ReasonsEnum.NO_ADULT);

                eligibilityResponse.setEligible(false);
                eligibilityResponse.setReasons(reasons);

                saveResult(clientId, correlationId, Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);

                return eligibilityResponse;
            }

        } catch (Exception e) {
            log.error("Error occurred while evaluating eligibility! CliendId: {}, CorrelationId: {}, Error message: {}", clientId, correlationId, e.getMessage(), e);
            saveResult(clientId, correlationId, Eligibility.EligibilityResultEnum.ERROR);

            throw new EligibilityApiException(correlationId, e.getMessage(), e);
        }
    }

    private Eligibility saveResult(String clientId, String correlationId, Eligibility.EligibilityResultEnum result) {
        return eligibilityRepository.save(
                Eligibility.builder()
                        .clientId(clientId)
                        .correlationId(correlationId)
                        .result(result)
                        .build()
        );
    }
}
