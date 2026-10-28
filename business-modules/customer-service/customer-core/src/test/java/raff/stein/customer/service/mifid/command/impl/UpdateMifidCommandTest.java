package raff.stein.customer.service.mifid.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.bo.mifid.config.MifidAnswerOption;
import raff.stein.customer.model.bo.mifid.config.MifidQuestion;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;
import raff.stein.customer.model.entity.mifid.MifidAnswerOptionEntity;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.MifidResponseEntity;
import raff.stein.customer.repository.mifid.MifidAnswerOptionRepository;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionRepository;
import raff.stein.customer.repository.mifid.MifidResponseRepository;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateMifidCommandTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final Long FILLING_ID = 22L;

    private MifidFillingRepository mifidFillingRepository;
    private MifidResponseRepository mifidResponseRepository;
    private MifidAnswerOptionRepository mifidAnswerOptionRepository;
    private MifidQuestionRepository mifidQuestionRepository;
    private UpdateMifidCommand updateMifidCommand;

    @BeforeEach
    void setUp() {
        mifidFillingRepository = mock(MifidFillingRepository.class);
        mifidResponseRepository = mock(MifidResponseRepository.class);
        mifidAnswerOptionRepository = mock(MifidAnswerOptionRepository.class);
        mifidQuestionRepository = mock(MifidQuestionRepository.class);
        updateMifidCommand = new UpdateMifidCommand(
                mifidFillingRepository,
                mifidResponseRepository,
                mifidAnswerOptionRepository,
                mifidQuestionRepository,
                mock(SaveMifidCommand.class));
        when(mifidQuestionRepository.findByIdIn(anyList())).thenReturn(List.of());
    }

    @Test
    void shouldUpdateExistingResponseMatchedByQuestionWithoutReplacingManagedCollection() {
        final MifidResponseEntity storedResponse = MifidResponseEntity.builder()
                .id(100L).questionId(1L).answerOptionId(10L).fillingId(FILLING_ID).build();
        final Set<MifidResponseEntity> managedResponses = new HashSet<>(Set.of(storedResponse));
        final MifidFillingEntity storedFilling = MifidFillingEntity.builder()
                .id(FILLING_ID).customerId(CUSTOMER_ID).questionnaireId(1L).responses(managedResponses).build();
        when(mifidFillingRepository.findByIdAndCustomerId(FILLING_ID, CUSTOMER_ID)).thenReturn(Optional.of(storedFilling));
        when(mifidAnswerOptionRepository.findByIdIn(anyList()))
                .thenReturn(List.of(MifidAnswerOptionEntity.builder().id(11L).score(7).build()));

        // client re-sends the answer without a response id: it must update, not duplicate
        final MifidFilling input = MifidFilling.builder()
                .fillingId(FILLING_ID)
                .responses(List.of(MifidResponse.builder()
                        .question(MifidQuestion.builder().id(1L).build())
                        .answerOption(MifidAnswerOption.builder().id(11L).build())
                        .build()))
                .build();

        final MifidFilling result = updateMifidCommand.execute(CUSTOMER_ID, input);

        assertSame(managedResponses, storedFilling.getResponses());
        assertEquals(1, storedFilling.getResponses().size());
        assertEquals(11L, storedResponse.getAnswerOptionId());
        assertEquals(1, result.getResponses().size());
    }

    @Test
    void shouldAddNewResponseToManagedCollection() {
        final Set<MifidResponseEntity> managedResponses = new HashSet<>();
        final MifidFillingEntity storedFilling = MifidFillingEntity.builder()
                .id(FILLING_ID).customerId(CUSTOMER_ID).questionnaireId(1L).responses(managedResponses).build();
        when(mifidFillingRepository.findByIdAndCustomerId(FILLING_ID, CUSTOMER_ID)).thenReturn(Optional.of(storedFilling));
        when(mifidAnswerOptionRepository.findByIdIn(anyList())).thenReturn(List.of());
        when(mifidResponseRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final MifidFilling input = MifidFilling.builder()
                .fillingId(FILLING_ID)
                .responses(List.of(MifidResponse.builder()
                        .question(MifidQuestion.builder().id(2L).build())
                        .answerOption(MifidAnswerOption.builder().id(20L).build())
                        .build()))
                .build();

        updateMifidCommand.execute(CUSTOMER_ID, input);

        assertSame(managedResponses, storedFilling.getResponses());
        assertEquals(1, managedResponses.size());
        assertEquals(FILLING_ID, managedResponses.iterator().next().getFillingId());
    }

    @Test
    void shouldThrowNotFoundWhenFillingDoesNotBelongToCustomer() {
        when(mifidFillingRepository.findByIdAndCustomerId(FILLING_ID, CUSTOMER_ID)).thenReturn(Optional.empty());
        final MifidFilling input = MifidFilling.builder().fillingId(FILLING_ID).responses(List.of()).build();

        assertThrows(GenericObjectNotFoundException.class, () -> updateMifidCommand.execute(CUSTOMER_ID, input));
    }
}
