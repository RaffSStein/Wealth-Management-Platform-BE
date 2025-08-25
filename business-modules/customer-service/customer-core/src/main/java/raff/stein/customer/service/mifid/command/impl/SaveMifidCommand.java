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
import raff.stein.customer.model.entity.mifid.mapper.MifidFillingEntityToMifidFillingMapper;
import raff.stein.customer.model.entity.mifid.mapper.MifidResponseEntityToMifidResponseMapper;
import raff.stein.customer.repository.mifid.MifidAnswerOptionRepository;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionRepository;
import raff.stein.customer.repository.mifid.MifidResponseRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.command.MifidCommand;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaveMifidCommand implements MifidCommand {

    private final MifidFillingRepository mifidFillingRepository;
    private final MifidResponseRepository mifidResponseRepository;
    private final MifidAnswerOptionRepository mifidAnswerOptionRepository;
    private final MifidQuestionRepository mifidQuestionRepository;

    private static final MifidFillingEntityToMifidFillingMapper mifidFillingEntityToMifidFillingMapper =
            MifidFillingEntityToMifidFillingMapper.MAPPER;
    private static final MifidResponseEntityToMifidResponseMapper mifidResponseEntityToMifidResponseMapper =
            MifidResponseEntityToMifidResponseMapper.MAPPER;

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.SAVE;
    }

    //TODO: probably to refactor. Remove saving logics were possible, since the whole process is transactional
    // the filling status is set in posthooks of the workflow
    @Override
    public MifidFilling execute(@NonNull UUID customerId, @Nullable MifidFilling mifidFilling) {
        if(mifidFilling == null) {
            log.warn("Mifid filling is null, cannot save");
            return null;
        }
        // save operation is meant to be executed once per filling instance
        // create new entity from BO
        MifidFillingEntity mifidFillingEntity = mifidFillingEntityToMifidFillingMapper.toMifidFillingEntity(mifidFilling);
        mifidFillingEntity.initializeMifidFirstSave();
        mifidFillingEntity.setCustomerId(customerId);
        // save filling entity
        MifidFillingEntity savedMifidFillingEntity = mifidFillingRepository.save(mifidFillingEntity);
        // save also answers
        if (mifidFilling.getResponses() != null && !mifidFilling.getResponses().isEmpty()) {
            List<MifidResponse> responses = mifidFilling.getResponses();

            // build entities map for questions and answer options
            Map<Long, MifidQuestionEntity> questionEntityMap = mifidQuestionRepository
                    .findByIdIn(
                            responses
                                    .stream()
                                    .map(r -> r.getQuestion().getId())
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .toList())
                    .stream()
                    .collect(Collectors.toMap(MifidQuestionEntity::getId, q -> q));
            Map<Long, MifidAnswerOptionEntity> answerOptionEntityMap = mifidAnswerOptionRepository
                    .findByIdIn(
                            responses
                                    .stream()
                                    .map(r -> r.getAnswerOption().getId())
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .toList())
                    .stream()
                    .collect(Collectors.toMap(MifidAnswerOptionEntity::getId, a -> a));

            List<MifidResponseEntity> responseEntities = responses
                    .stream()
                    .map(mifidResponseEntityToMifidResponseMapper::toMifidResponseEntity)
                    .toList();
            responseEntities.forEach(responseEntity -> responseEntity.setFillingId(savedMifidFillingEntity.getId()));
            List<MifidResponseEntity> savedResponses = mifidResponseRepository.saveAll(responseEntities);
            // setting question and answer option entities, only for mapping purpose, hibernate won't persist them
            savedResponses.forEach(responseEntity -> {
                if (responseEntity.getQuestionId() != null) {
                    responseEntity.setQuestion(questionEntityMap.getOrDefault(responseEntity.getQuestionId(), null));
                }
                if (responseEntity.getAnswerOptionId() != null) {
                    responseEntity.setAnswerOption(answerOptionEntityMap.getOrDefault(responseEntity.getAnswerOptionId(), null));
                }
            });
            savedMifidFillingEntity.setResponses(new HashSet<>(savedResponses));
        }
        // convert back to BO and return
        return mifidFillingEntityToMifidFillingMapper.toMifidFilling(savedMifidFillingEntity);
    }
}
