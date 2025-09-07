package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.MifidApi;
import org.openapitools.model.MifidFillingDTO;
import org.openapitools.model.MifidQuestionnaireConfigDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.customer.controller.mapper.mifid.MifidConfigDTOToMifidConfig;
import raff.stein.customer.controller.mapper.mifid.MifidFillingToMifidFillingDTOMapper;
import raff.stein.customer.model.bo.mifid.config.MifidQuestionnaireConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
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
    public ResponseEntity<MifidQuestionnaireConfigDTO> getLatestValidMifidQuestionnaireConfig() {
        MifidQuestionnaireConfig mifidConfig = mifidService.getLatestValidMifidQuestionnaireConfig();
        MifidQuestionnaireConfigDTO mifidQuestionnaireConfigDTO = mifidConfigDTOToMifidConfig.toMifidConfigDTO(mifidConfig);
        return ResponseEntity.ok(mifidQuestionnaireConfigDTO);
    }

    @Override
    public ResponseEntity<MifidFillingDTO> getMifidFillingForCustomer(
            UUID customerId,
            Long fillingId) {
        final MifidFilling mifidFilling = mifidService.handleFilling(
                MifidActionType.GET,
                customerId,
                null);
        final MifidFillingDTO outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }

    @Override
    public ResponseEntity<MifidFillingDTO> saveMifidFillingForCustomer(
            UUID customerId,
            MifidFillingDTO mifidFillingDTO) {
        final MifidFilling mifidFilling = mifidService.handleFilling(
                MifidActionType.SAVE,
                customerId,
                mifidFillingDTO);
        final MifidFillingDTO outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }

    @Override
    public ResponseEntity<MifidFillingDTO> updateMifidFillingForCustomer(
            UUID customerId,
            Long fillingId,
            MifidFillingDTO mifidFillingDTO) {
        final MifidFilling mifidFilling = mifidService.handleFilling(
                MifidActionType.UPDATE,
                customerId,
                mifidFillingDTO);
        final MifidFillingDTO outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }

    @Override
    public ResponseEntity<MifidFillingDTO> submitQuestionnaire(
            UUID customerId,
            MifidFillingDTO mifidFillingDTO) {
        final MifidFilling mifidFilling = mifidService.handleFilling(
                MifidActionType.SUBMIT,
                customerId,
                mifidFillingDTO);
        final MifidFillingDTO outputDTO = mifidFillingToMifidFillingDTOMapper.toMifidFillingDTO(mifidFilling);
        return ResponseEntity.ok(outputDTO);
    }
}
