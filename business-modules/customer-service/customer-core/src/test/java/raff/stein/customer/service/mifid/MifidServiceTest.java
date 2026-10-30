package raff.stein.customer.service.mifid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.bo.mifid.config.MifidQuestionnaireConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.MifidQuestionnaireEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.workflow.MifidWorkflowDispatcher;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MifidServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private MifidQuestionnaireRepository mifidQuestionnaireRepository;
    private MifidFillingRepository mifidFillingRepository;
    private MifidService mifidService;

    @BeforeEach
    void setUp() {
        mifidQuestionnaireRepository = mock(MifidQuestionnaireRepository.class);
        mifidFillingRepository = mock(MifidFillingRepository.class);
        mifidService = new MifidService(mock(MifidWorkflowDispatcher.class), mifidQuestionnaireRepository, mifidFillingRepository);
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

    @Test
    void shouldReturnLatestSubmittedFillingWithRiskProfile() {
        final MifidFillingEntity submitted = MifidFillingEntity.builder()
                .id(5L)
                .customerId(CUSTOMER_ID)
                .questionnaireId(1L)
                .status(MifidFillingStatus.SUBMITTED)
                .calculatedRiskProfile("BALANCED")
                .riskScore(42)
                .responses(new HashSet<>())
                .build();
        when(mifidFillingRepository.findTopByCustomerIdAndStatusOrderByIdDesc(CUSTOMER_ID, MifidFillingStatus.SUBMITTED))
                .thenReturn(Optional.of(submitted));

        final MifidFilling result = mifidService.getCurrentSubmittedMifidFilling(CUSTOMER_ID);

        assertEquals(5L, result.getFillingId());
        assertEquals(MifidFillingStatus.SUBMITTED, result.getStatus());
        assertEquals("BALANCED", result.getCustomerRiskProfile().getProfile());
    }

    @Test
    void shouldThrowNotFoundWhenCustomerHasNoSubmittedFilling() {
        when(mifidFillingRepository.findTopByCustomerIdAndStatusOrderByIdDesc(CUSTOMER_ID, MifidFillingStatus.SUBMITTED))
                .thenReturn(Optional.empty());

        assertThrows(GenericObjectNotFoundException.class, () -> mifidService.getCurrentSubmittedMifidFilling(CUSTOMER_ID));
    }
}
