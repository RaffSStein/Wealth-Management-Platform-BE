package raff.stein.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.entity.DocumentTypeEntity;
import raff.stein.document.model.entity.mapper.DocumentTypeEntityToDocumentTypeMapper;
import raff.stein.document.repository.DocumentTypeRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;
    private static final DocumentTypeEntityToDocumentTypeMapper documentTypeEntityToDocumentTypeMapper =
            DocumentTypeEntityToDocumentTypeMapper.MAPPER;

    public DocumentType getDocumentType(String documentTypeName) {
        return documentTypeEntityToDocumentTypeMapper.toDocumentType(getDocumentTypeEntity(documentTypeName));
    }

    public DocumentTypeEntity getDocumentTypeEntity(String documentTypeName) {
        if(documentTypeName == null || documentTypeName.isBlank()) {
            return null;
        }
        String normalizedDocumentTypeName = documentTypeName.trim().toUpperCase();
        Optional<DocumentTypeEntity> documentTypeEntityOptional =
                documentTypeRepository.findByTypeName(normalizedDocumentTypeName);
        if (documentTypeEntityOptional.isEmpty()) {
            log.warn("Document type [{}] not found", normalizedDocumentTypeName);
            return null;
        }
        return documentTypeEntityOptional.get();
    }
}
