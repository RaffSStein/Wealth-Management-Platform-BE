package raff.stein.customer.service.mifid.command.impl;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;
import raff.stein.customer.model.entity.mifid.RiskProfileConfigurationEntity;
import raff.stein.customer.repository.mifid.RiskProfileConfigurationRepository;
import raff.stein.customer.service.mifid.command.MifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitMifidCommand implements MifidCommand {

    private final RiskProfileConfigurationRepository riskProfileConfigurationRepository;

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.SUBMIT;
    }

    @Override
    public MifidFilling execute(@NonNull UUID customerId, @Nullable MifidFilling mifidFilling) {
        // answers to all questions are checked in pre hook of SubmitMifidWorkflow
        // mifid filling status is set in post hook of SubmitMifidWorkflow
        // just calculate customer risk profile here
        if(mifidFilling == null) {
            throw new IllegalArgumentException("MifidFilling cannot be null for action type: " + getActionType());
        }
        // 1. retrieve all risk profile configurations for this questionnaire version
        List<RiskProfileConfigurationEntity> configurations =
                riskProfileConfigurationRepository.findByQuestionnaireId(mifidFilling.getQuestionnaireId());
        if(configurations.isEmpty()) {
            throw new IllegalStateException("No risk profile configurations found for questionnaire with ID: " + mifidFilling.getQuestionnaireId());
        }
        // 2. calculate risk profile based on answers and configurations
        final List<MifidResponse> responses = mifidFilling.getResponses();
        final int totalScore = responses.stream()
                .filter(response -> response.getAnswerOption() != null && response.getAnswerOption().getScore() != null)
                .mapToInt(response -> response.getAnswerOption().getScore())
                .sum();
        // 3. determine risk profile based on total score
        final String riskProfile = configurations.stream()
                .filter(config -> totalScore >= config.getMinScore() && totalScore <= config.getMaxScore())
                .findFirst()
                .map(RiskProfileConfigurationEntity::getProfileLabel)
                .orElse("UNDEFINED");
        log.info("Calculated risk profile: {} with score: {} for customer with ID: {}", riskProfile, totalScore, customerId);
        // 4. set risk profile in mifid filling
        mifidFilling.setCustomerRiskProfile(
                CustomerRiskProfile.builder()
                        .profile(riskProfile)
                        .score(totalScore)
                        .build());
        return mifidFilling;
    }
}
