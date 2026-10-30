package raff.stein.customer.service.mifid.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetMifidCommandTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private MifidFillingRepository mifidFillingRepository;
    private GetMifidCommand getMifidCommand;

    @BeforeEach
    void setUp() {
        mifidFillingRepository = mock(MifidFillingRepository.class);
        getMifidCommand = new GetMifidCommand(mifidFillingRepository);
    }

    @Test
    void shouldReturnRequestedFillingWhenFillingIdIsProvided() {
        when(mifidFillingRepository.findByIdAndCustomerId(3L, CUSTOMER_ID))
                .thenReturn(Optional.of(filling(3L, MifidFillingStatus.SUBMITTED)));

        final MifidFilling result = getMifidCommand.execute(CUSTOMER_ID, MifidFilling.builder().fillingId(3L).build());

        assertEquals(3L, result.getFillingId());
        assertEquals(MifidFillingStatus.SUBMITTED, result.getStatus());
    }

    @Test
    void shouldReturnLatestNonDeprecatedFillingWhenNoFillingIdIsProvided() {
        when(mifidFillingRepository.findTopByCustomerIdAndStatusNotOrderByIdDesc(CUSTOMER_ID, MifidFillingStatus.DEPRECATED))
                .thenReturn(Optional.of(filling(7L, MifidFillingStatus.DRAFT)));

        final MifidFilling result = getMifidCommand.execute(CUSTOMER_ID, null);

        assertEquals(7L, result.getFillingId());
    }

    @Test
    void shouldThrowNotFoundWhenFillingBelongsToAnotherCustomer() {
        when(mifidFillingRepository.findByIdAndCustomerId(3L, CUSTOMER_ID)).thenReturn(Optional.empty());

        final MifidFilling input = MifidFilling.builder().fillingId(3L).build();
        assertThrows(GenericObjectNotFoundException.class, () -> getMifidCommand.execute(CUSTOMER_ID, input));
    }

    @Test
    void shouldThrowNotFoundWhenCustomerHasNoFilling() {
        when(mifidFillingRepository.findTopByCustomerIdAndStatusNotOrderByIdDesc(CUSTOMER_ID, MifidFillingStatus.DEPRECATED))
                .thenReturn(Optional.empty());

        assertThrows(GenericObjectNotFoundException.class, () -> getMifidCommand.execute(CUSTOMER_ID, null));
    }

    private static MifidFillingEntity filling(Long id, MifidFillingStatus status) {
        return MifidFillingEntity.builder()
                .id(id)
                .customerId(CUSTOMER_ID)
                .questionnaireId(1L)
                .status(status)
                .responses(new HashSet<>())
                .build();
    }
}
