package raff.stein.customer.service.mifid.command.impl;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.config.MifidAnswerOption;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.MifidResponseEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.model.entity.mifid.mapper.MifidFillingEntityToMifidFillingMapper;
import raff.stein.customer.model.entity.mifid.mapper.MifidResponseEntityToMifidResponseMapper;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.command.MifidCommand;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateMifidCommand implements MifidCommand {

    private final MifidFillingRepository mifidFillingRepository;

    private final SaveMifidCommand saveMifidCommand;

    private static final MifidFillingEntityToMifidFillingMapper mifidFillingEntityToMifidFillingMapper =
            MifidFillingEntityToMifidFillingMapper.MAPPER;
    private static final MifidResponseEntityToMifidResponseMapper mifidResponseEntityToMifidResponseMapper =
            MifidResponseEntityToMifidResponseMapper.MAPPER;

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.UPDATE;
    }

    @Override
    public MifidFilling execute(@NonNull UUID customerId, @Nullable MifidFilling mifidFilling) {
        // retrieve the latest valid MifidFilling for the given customerId
        var latestFillingOpt = mifidFillingRepository.findTopByCustomerIdAndStatusNotOrderByFillingDateDesc(customerId, MifidFillingStatus.DEPRECATED);
        // if no filling is provided from FE, just return the latest from DB
        if (mifidFilling == null) {
            log.warn("Called UpdateMifidCommand with null MifidFilling, returning latest filling from DB...");
            return latestFillingOpt.map(mifidFillingEntityToMifidFillingMapper::toMifidFilling).orElse(null);
        }
        // perform upsert based on FE provided filling
        return latestFillingOpt
                .map(latestFilling -> updateFilling(latestFilling, mifidFilling))
                .orElseGet(() -> insertFilling(customerId, mifidFilling));
    }

    private MifidFilling insertFilling(@NonNull UUID customerId, MifidFilling mifidFilling) {
        log.info("No existing Mifid filling found for customer with ID: {}, inserting new filling...", customerId);
        return saveMifidCommand.execute(customerId, mifidFilling);
    }


    private MifidFilling updateFilling(MifidFillingEntity latestFillingFromDB, MifidFilling mifidFillingFromFE) {
        log.info("Existing Mifid filling found for customer with ID: {}, updating filling...", latestFillingFromDB.getCustomerId());
        // we need to update existing responses and insert new ones
        Map<Long, MifidResponse> responsesFromFEToUpdateById = mifidFillingFromFE.getResponses()
                .stream()
                .filter(response -> response.getId() != null)
                .collect(Collectors.toMap(MifidResponse::getId, response -> response));

        List<MifidResponse> responseFromFEToBeInserted = mifidFillingFromFE.getResponses()
                .stream()
                .filter(response -> response.getId() == null)
                .toList();

        Map<Long, MifidResponseEntity> responsesFromDBById = latestFillingFromDB.getResponses()
                .stream()
                .collect(Collectors.toMap(MifidResponseEntity::getId, response -> response));

        List<MifidResponseEntity> resultingResponses = new ArrayList<>();
        // update existing responses
        for(Map.Entry<Long, MifidResponse> entry : responsesFromFEToUpdateById.entrySet()) {
            Long responseId = entry.getKey();
            MifidResponse responseFromFE = entry.getValue();
            MifidResponseEntity responseFromDB = responsesFromDBById.get(responseId);
            // updating responses means to set the new selected answer option
            if(responseFromDB != null) {
                final MifidAnswerOption answerOptionFromFE = responseFromFE.getAnswerOption();
                // update answer option to actual entity
                responseFromDB.setAnswerOptionId(answerOptionFromFE.getId());
                resultingResponses.add(responseFromDB);
            } else {
                log.warn("Response with ID: {} not found in DB for filling ID: {}, skipping update for this response.", responseId, latestFillingFromDB.getId());
            }
        }
        // add new responses
        List<MifidResponseEntity> newResponseEntities = responseFromFEToBeInserted
                .stream()
                .map(mifidResponseEntityToMifidResponseMapper::toMifidResponseEntity)
                .toList();
        newResponseEntities.forEach(e -> e.setFillingId(latestFillingFromDB.getId()));
        resultingResponses.addAll(newResponseEntities);
        // set resulting responses to filling entity, for mapping purpose
        // hibernate won't persist them as they are marked as not insertable/updatable
        latestFillingFromDB.setResponses(new HashSet<>(resultingResponses));
        return mifidFillingEntityToMifidFillingMapper.toMifidFilling(latestFillingFromDB);
    }

}
