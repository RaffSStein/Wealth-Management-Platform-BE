package raff.stein.document.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.DocumentApi;
import org.openapitools.model.DocumentDTO;
import org.openapitools.model.FileDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import raff.stein.document.controller.mapper.DocumentDTOToDocumentMapper;
import raff.stein.document.controller.mapper.FileDTOToFileMapper;
import raff.stein.document.model.Document;
import raff.stein.document.model.File;
import raff.stein.document.service.DocumentService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentApi {

    private final DocumentService documentService;
    private static final FileDTOToFileMapper fileDTOToFileMapper = FileDTOToFileMapper.MAPPER;
    private static final DocumentDTOToDocumentMapper documentDTOToDocumentMapper = DocumentDTOToDocumentMapper.MAPPER;

    @Override
    public ResponseEntity<DocumentDTO> downloadDocument(UUID documentId) {
        if (documentId == null) {
            return ResponseEntity.badRequest().build();
        }
        Document document = documentService.downloadDocument(documentId);
        DocumentDTO responseDocumentDTO = documentDTOToDocumentMapper.toDocumentDTO(document);
        return ResponseEntity.ok(responseDocumentDTO);
    }

    @Override
    public ResponseEntity<DocumentDTO> uploadDocument(MultipartFile multipartFile, FileDTO fileDTO) {
        File fileInput = fileDTOToFileMapper.toNewFile(fileDTO, multipartFile);
        Document createdDocument = documentService.uploadDocument(fileInput);
        DocumentDTO responseDocumentDTO = documentDTOToDocumentMapper.toDocumentDTO(createdDocument);
        return ResponseEntity.ok(responseDocumentDTO);
    }
}
