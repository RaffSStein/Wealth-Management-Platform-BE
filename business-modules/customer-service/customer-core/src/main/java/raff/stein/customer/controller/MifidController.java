package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import io.openapiprocessor.openapi.api.MifidApi;
import io.openapiprocessor.openapi.model.MifidFillingDto;
import io.openapiprocessor.openapi.model.MifidQuestionnaireConfigDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.customer.controller.mapper.mifid.MifidConfigDTOToMifidConfig;
import raff.stein.customer.controller.mapper.mifid.MifidFillingToMifidFillingDTOMapper;
import raff.stein.customer.model.bo.mifid.config.MifidQuestionnaireConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.controller.access.CustomerAccess;
import raff.stein.customer.controller.access.CustomerAccessLevel;
import raff.stein.customer.service.mifid.MifidService;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MifidController implements MifidApi {

    private final MifidService mifidService;

    private static final MifidConfigDTOToMifidConfig mifidConfigDTOToMifidConfig = MifidConfigDTOToMifidConfig.MAPPER;
    private static final MifidFillingToMifidFillingDTOMapper mifidFillingToMifidFillingDTOMapper = MifidFillingToMifidFillingDTOMapper.MAPPER;

    @Override
    public ResponseEntity<MifidQuestionnaireConfigDto> getLatestValidMifidQuestionnaireConfig() {
        MifidQuestionnaireConfig mifidConfig = mifidService.getLatestValidMifidQuestionnaireConfig();
        MifidQuestionnaireConfigDto mifidQuestionnaireConfigDTO = mifidConfigDTOToMifidConfig.toMifidConfigDTO(mifidConfig);
        return ResponseEntity.ok(mifidQuestionnaireConfigDTO);
    }

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<MifidFillingDto> getMifidFillingForCustomer(
            UUID customerId,
            Long fillingId) {
        final MifidFilling mifidFilling = mifidService.getMifidFilling(customerId, fillingId);
        final MifidFillingDto outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<MifidFillingDto> getCurrentMifidFillingForCustomer(UUID customerId) {
        final MifidFilling mifidFilling = mifidService.getCurrentSubmittedMifidFilling(customerId);
        final MifidFillingDto outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }

    @CustomerAccess(CustomerAccessLevel.WRITE)
    @Override
    public ResponseEntity<MifidFillingDto> saveMifidFillingForCustomer(
            UUID customerId,
            MifidFillingDto mifidFillingDTO) {
        final MifidFilling mifidFilling = mifidService.handleFilling(
                MifidActionType.SAVE,
                customerId,
                mifidFillingDTO);
        final MifidFillingDto outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }

    @CustomerAccess(CustomerAccessLevel.WRITE)
    @Override
    public ResponseEntity<MifidFillingDto> updateMifidFillingForCustomer(
            UUID customerId,
            Long fillingId,
            MifidFillingDto mifidFillingDTO) {
        // the path is authoritative over any fillingId sent in the body
        final MifidFilling mifidFilling = mifidService.handleFilling(
                MifidActionType.UPDATE,
                customerId,
                fillingId,
                mifidFillingDTO);
        final MifidFillingDto outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }

    @CustomerAccess(CustomerAccessLevel.WRITE)
    @Override
    public ResponseEntity<MifidFillingDto> submitQuestionnaire(
            UUID customerId,
            MifidFillingDto mifidFillingDTO) {
        final MifidFilling mifidFilling = mifidService.handleFilling(
                MifidActionType.SUBMIT,
                customerId,
                mifidFillingDTO);
        final MifidFillingDto outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }
}
