package raff.stein.customer.service.mifid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.bo.mifid.config.MifidQuestionnaireConfig;
import raff.stein.customer.model.entity.mifid.MifidQuestionnaireEntity;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.workflow.MifidWorkflowDispatcher;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MifidServiceTest {

    private MifidQuestionnaireRepository mifidQuestionnaireRepository;
    private MifidService mifidService;

    @BeforeEach
    void setUp() {
        mifidQuestionnaireRepository = mock(MifidQuestionnaireRepository.class);
        mifidService = new MifidService(mock(MifidWorkflowDispatcher.class), mifidQuestionnaireRepository);
    }

    @Test
    void shouldReturnValidQuestionnaireConfig() {
        final MifidQuestionnaireEntity entity = MifidQuestionnaireEntity.builder()
                .id(1L)
                .name("Mifid 2025")
                .validFrom(LocalDate.of(2025, 1, 1))
                .validTo(LocalDate.of(2099, 12, 31))
                .questionnaireVersion("1.0")
                .status("ACTIVE")
                .build();
        when(mifidQuestionnaireRepository.findValidQuestionnaire(any(LocalDate.class))).thenReturn(Optional.of(entity));

        final MifidQuestionnaireConfig config = mifidService.getLatestValidMifidQuestionnaireConfig();

        assertEquals("Mifid 2025", config.getName());
    }

    @Test
    void shouldThrowNotFoundWhenNoQuestionnaireIsValidToday() {
        when(mifidQuestionnaireRepository.findValidQuestionnaire(any(LocalDate.class))).thenReturn(Optional.empty());

        assertThrows(GenericObjectNotFoundException.class, () -> mifidService.getLatestValidMifidQuestionnaireConfig());
    }
}
