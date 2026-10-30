package raff.stein.customer.service.mifid.command.impl;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;
import raff.stein.customer.model.entity.mifid.MifidAnswerOptionEntity;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.MifidQuestionEntity;
import raff.stein.customer.model.entity.mifid.MifidResponseEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.model.entity.mifid.mapper.MifidFillingEntityToMifidFillingMapper;
import raff.stein.customer.model.entity.mifid.mapper.MifidResponseEntityToMifidResponseMapper;
import raff.stein.customer.repository.mifid.MifidAnswerOptionRepository;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionRepository;
import raff.stein.customer.repository.mifid.MifidResponseRepository;
import raff.stein.customer.service.mifid.command.MifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateMifidCommand implements MifidCommand {

    private final MifidFillingRepository mifidFillingRepository;
    private final MifidResponseRepository mifidResponseRepository;
    private final MifidAnswerOptionRepository mifidAnswerOptionRepository;
    private final MifidQuestionRepository mifidQuestionRepository;

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
        // if no filling is provided from FE, just return the latest from DB
        if (mifidFilling == null) {
            log.warn("Called UpdateMifidCommand with null MifidFilling, returning latest filling from DB...");
            return mifidFillingRepository
                    .findTopByCustomerIdAndStatusNotOrderByIdDesc(customerId, MifidFillingStatus.DEPRECATED)
                    .map(mifidFillingEntityToMifidFillingMapper::toMifidFilling)
                    .orElse(null);
        }
        // the filling identified by fillingId when provided, otherwise the latest non-deprecated one
        final Optional<MifidFillingEntity> targetFillingOpt = mifidFilling.getFillingId() != null
                ? Optional.of(mifidFillingRepository.findByIdAndCustomerId(mifidFilling.getFillingId(), customerId)
                        .orElseThrow(GenericObjectNotFoundException.with(String.valueOf(mifidFilling.getFillingId()))))
                : mifidFillingRepository.findTopByCustomerIdAndStatusNotOrderByIdDesc(customerId, MifidFillingStatus.DEPRECATED);
        // perform upsert based on FE provided filling
        return targetFillingOpt
                .map(targetFilling -> updateFilling(targetFilling, mifidFilling))
                .orElseGet(() -> insertFilling(customerId, mifidFilling));
    }

    private MifidFilling insertFilling(@NonNull UUID customerId, MifidFilling mifidFilling) {
        log.info("No existing Mifid filling found for customer with ID: [{}], inserting new filling...", customerId);
        return saveMifidCommand.execute(customerId, mifidFilling);
    }

    private MifidFilling updateFilling(MifidFillingEntity fillingFromDB, MifidFilling mifidFillingFromFE) {
        log.info("Updating Mifid filling with ID: [{}] for customer with ID: [{}]", fillingFromDB.getId(), fillingFromDB.getCustomerId());
        final List<MifidResponse> responsesFromFE = Optional.ofNullable(mifidFillingFromFE.getResponses()).orElse(List.of());

        // stored responses, matched by response id first and by question id as a fallback,
        // so a client re-sending answers without ids updates them instead of duplicating them
        final Map<Long, MifidResponseEntity> responsesFromDBById = fillingFromDB.getResponses()
                .stream()
                .collect(Collectors.toMap(MifidResponseEntity::getId, Function.identity()));
        final Map<Long, MifidResponseEntity> responsesFromDBByQuestionId = fillingFromDB.getResponses()
                .stream()
                .filter(response -> response.getQuestionId() != null)
                .collect(Collectors.toMap(MifidResponseEntity::getQuestionId, Function.identity(), (first, second) -> first));

        final Map<Long, MifidAnswerOptionEntity> answerOptionEntityMap = mifidAnswerOptionRepository
                .findByIdIn(responsesFromFE.stream()
                        .map(MifidResponse::getAnswerOption)
                        .filter(Objects::nonNull)
                        .map(answerOption -> answerOption.getId())
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(MifidAnswerOptionEntity::getId, Function.identity()));

        final List<MifidResponse> responsesToInsert = new ArrayList<>();
        for (MifidResponse responseFromFE : responsesFromFE) {
            final Long questionId = responseFromFE.getQuestion() != null ? responseFromFE.getQuestion().getId() : null;
            final MifidResponseEntity responseFromDB = Optional.ofNullable(responseFromFE.getId())
                    .map(responsesFromDBById::get)
                    .orElseGet(() -> questionId != null ? responsesFromDBByQuestionId.get(questionId) : null);
            if (responseFromDB != null) {
                // the managed entity is dirty-checked: no explicit save needed
                updateResponse(responseFromDB, responseFromFE, answerOptionEntityMap);
            } else if (responseFromFE.getId() != null) {
                log.warn("Response with ID: [{}] not found in filling with ID: [{}], skipping it", responseFromFE.getId(), fillingFromDB.getId());
            } else {
                responsesToInsert.add(responseFromFE);
            }
        }

        if (!responsesToInsert.isEmpty()) {
            final Map<Long, MifidQuestionEntity> questionEntityMap = mifidQuestionRepository
                    .findByIdIn(responsesToInsert.stream()
                            .map(MifidResponse::getQuestion)
                            .filter(Objects::nonNull)
                            .map(question -> question.getId())
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList())
                    .stream()
                    .collect(Collectors.toMap(MifidQuestionEntity::getId, Function.identity()));

            final List<MifidResponseEntity> newResponseEntities = responsesToInsert
                    .stream()
                    .map(mifidResponseEntityToMifidResponseMapper::toMifidResponseEntity)
                    .toList();
            newResponseEntities.forEach(responseEntity -> responseEntity.setFillingId(fillingFromDB.getId()));
            final List<MifidResponseEntity> savedResponses = mifidResponseRepository.saveAll(newResponseEntities);
            // setting question and answer option entities, only for mapping purpose, hibernate won't persist them
            savedResponses.forEach(responseEntity -> {
                responseEntity.setQuestion(questionEntityMap.get(responseEntity.getQuestionId()));
                responseEntity.setAnswerOption(answerOptionEntityMap.get(responseEntity.getAnswerOptionId()));
            });
            // mutate the managed collection: replacing it breaks orphanRemoval
            fillingFromDB.getResponses().addAll(savedResponses);
        }
        return mifidFillingEntityToMifidFillingMapper.toMifidFilling(fillingFromDB);
    }

    private void updateResponse(
            MifidResponseEntity responseFromDB,
            MifidResponse responseFromFE,
            Map<Long, MifidAnswerOptionEntity> answerOptionEntityMap) {
        final Long answerOptionId = responseFromFE.getAnswerOption() != null ? responseFromFE.getAnswerOption().getId() : null;
        responseFromDB.setAnswerOptionId(answerOptionId);
        // keep the association aligned with the id for the response mapping
        responseFromDB.setAnswerOption(answerOptionId != null ? answerOptionEntityMap.get(answerOptionId) : null);
        responseFromDB.setFreeText(responseFromFE.getFreeText());
        responseFromDB.setNumericValue(responseFromFE.getNumericValue());
        responseFromDB.setDateValue(responseFromFE.getDateValue());
    }
}
