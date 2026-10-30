package raff.stein.customer.service.mifid.workflow.step.state.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.bo.mifid.config.MifidAnswerOption;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.platformcore.exception.types.conflict.InvalidStateTransitionException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MifidStateHandlerTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final Long FILLING_ID = 24L;

    private MifidFillingRepository mifidFillingRepository;
    private MifidQuestionnaireRepository mifidQuestionnaireRepository;
    private SubmitMifidStateHandler submitMifidStateHandler;
    private UpdateMifidStateHandler updateMifidStateHandler;

    @BeforeEach
    void setUp() {
        mifidFillingRepository = mock(MifidFillingRepository.class);
        mifidQuestionnaireRepository = mock(MifidQuestionnaireRepository.class);
        submitMifidStateHandler = new SubmitMifidStateHandler(mifidFillingRepository, mifidQuestionnaireRepository);
        updateMifidStateHandler = new UpdateMifidStateHandler(mifidFillingRepository, mifidQuestionnaireRepository);
    }

    @Test
    void shouldRejectSubmitOfDraftFillingWithConflict() {
        assertThrows(InvalidStateTransitionException.class,
                () -> submitMifidStateHandler.validateStatus(MifidFillingStatus.DRAFT, MifidActionType.SUBMIT));
    }

    @Test
    void shouldRejectSubmitWhenNoFillingExists() {
        assertThrows(InvalidStateTransitionException.class,
                () -> submitMifidStateHandler.validateStatus(null, MifidActionType.SUBMIT));
    }

    @Test
    void shouldAllowUpdateWhenNoFillingExists() {
        assertDoesNotThrow(() -> updateMifidStateHandler.validateStatus(null, MifidActionType.UPDATE));
    }

    @Test
    void shouldAllowUpdateOfCompletedFilling() {
        assertDoesNotThrow(() -> updateMifidStateHandler.validateStatus(MifidFillingStatus.COMPLETED, MifidActionType.UPDATE));
    }

    @Test
    void shouldRejectUpdateOfSubmittedFillingWithConflict() {
        assertThrows(InvalidStateTransitionException.class,
                () -> updateMifidStateHandler.validateStatus(MifidFillingStatus.SUBMITTED, MifidActionType.UPDATE));
    }

    @Test
    void shouldRejectUpdateOfDeprecatedFillingWithConflict() {
        assertThrows(InvalidStateTransitionException.class,
                () -> updateMifidStateHandler.validateStatus(MifidFillingStatus.DEPRECATED, MifidActionType.UPDATE));
    }

    @Test
    void shouldSetCompletedWhenAllQuestionsAreAnswered() {
        final MifidFillingEntity storedFilling = MifidFillingEntity.builder()
                .id(FILLING_ID).customerId(CUSTOMER_ID).status(MifidFillingStatus.DRAFT).build();
        when(mifidFillingRepository.findByIdAndCustomerId(FILLING_ID, CUSTOMER_ID)).thenReturn(Optional.of(storedFilling));
        when(mifidQuestionnaireRepository.countQuestionsByQuestionnaireId(1L)).thenReturn(3L);
        final MifidFilling filling = fillingWithAnswers(3);

        updateMifidStateHandler.calculateAndSetNewStatus(filling, CUSTOMER_ID);

        assertEquals(MifidFillingStatus.COMPLETED, storedFilling.getStatus());
        assertEquals(MifidFillingStatus.COMPLETED, filling.getStatus());
    }

    @Test
    void shouldKeepDraftWhenSomeQuestionsAreUnanswered() {
        final MifidFillingEntity storedFilling = MifidFillingEntity.builder()
                .id(FILLING_ID).customerId(CUSTOMER_ID).status(MifidFillingStatus.DRAFT).build();
        when(mifidFillingRepository.findByIdAndCustomerId(FILLING_ID, CUSTOMER_ID)).thenReturn(Optional.of(storedFilling));
        when(mifidQuestionnaireRepository.countQuestionsByQuestionnaireId(1L)).thenReturn(3L);
        final MifidFilling filling = fillingWithAnswers(2);

        updateMifidStateHandler.calculateAndSetNewStatus(filling, CUSTOMER_ID);

        assertEquals(MifidFillingStatus.DRAFT, storedFilling.getStatus());
    }

    private MifidFilling fillingWithAnswers(int answers) {
        final List<MifidResponse> responses = IntStream.range(0, answers)
                .mapToObj(i -> MifidResponse.builder().answerOption(MifidAnswerOption.builder().id((long) i).build()).build())
                .toList();
        return MifidFilling.builder().fillingId(FILLING_ID).questionnaireId(1L).responses(responses).build();
    }
}
