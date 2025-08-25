package raff.stein.customer.service.mifid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.MifidFillingDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.controller.mapper.mifid.MifidFillingToMifidFillingDTOMapper;
import raff.stein.customer.exception.CustomerException;
import raff.stein.customer.model.bo.mifid.config.MifidQuestionnaireConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidQuestionnaireEntity;
import raff.stein.customer.model.entity.mifid.mapper.MifidQuestionnaireEntityToMifidQuestionnaireMapper;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.MifidWorkflowDispatcher;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MifidService {

    private final MifidWorkflowDispatcher mifidWorkflowDispatcher;

    private final MifidQuestionnaireRepository mifidQuestionnaireRepository;

    private static final MifidQuestionnaireEntityToMifidQuestionnaireMapper mifidQuestionnaireEntityToMifidQuestionnaireMapper =
            MifidQuestionnaireEntityToMifidQuestionnaireMapper.MAPPER;
    private static final MifidFillingToMifidFillingDTOMapper mifidFillingToMifidFillingDTOMapper =
            MifidFillingToMifidFillingDTOMapper.MAPPER;

    @Transactional(readOnly = true)
    public MifidQuestionnaireConfig getLatestValidMifidQuestionnaireConfig() {
        log.info("Fetching latest valid Mifid questionnaire configuration");
        MifidQuestionnaireEntity mifidQuestionnaireEntity = mifidQuestionnaireRepository.findValidQuestionnaire(LocalDate.now())
                .orElseThrow(() -> new CustomerException("No valid Mifid questionnaire configuration found for today date: " + LocalDate.now()));

        return mifidQuestionnaireEntityToMifidQuestionnaireMapper.toMifidQuestionnaireConfig(mifidQuestionnaireEntity);
    }

    @Transactional
    public MifidFilling handleFilling(
            MifidActionType actionType,
            UUID customerId,
            MifidFillingDTO dto) {
        if (actionType == null) {
            throw new IllegalArgumentException("Action type must not be null");
        }
        return mifidWorkflowDispatcher.dispatch(
                actionType,
                customerId,
                mifidFillingToMifidFillingDTOMapper.toMifidFilling(dto));
    }

}
