package raff.stein.document.service.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import raff.stein.document.exception.StorageException;
import raff.stein.document.model.Document;
import raff.stein.document.model.File;
import raff.stein.document.model.mapper.DocumentMapper;
import raff.stein.document.utils.DocumentFileNameUtils;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * Service implementation for Google Cloud Storage (GCS) integration.
 * <p>
 * This service provides methods to upload files to a GCS bucket and retrieve bucket information.
 * It is activated only when the Spring profile "gcs" is enabled.
 * <p>
 * Main responsibilities:
 * <ul>
 *   <li>Upload files to a configured GCS bucket</li>
 *   <li>Return metadata about the uploaded document</li>
 *   <li>Handle exceptions and logging during the upload process</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile("gcs")
public class GCSStorageService implements CloudStorageService {

    /**
     * The name of the GCS bucket where files will be stored.
     * Configured via the property 'gcp.cloud.storage.bucket-name'.
     */
    @Value("${gcp.cloud.storage.bucket-name}")
    private String bucketName;

    /**
     * The base directory inside the bucket where files will be uploaded.
     * Configured via the property 'gcp.cloud.storage.base-directory-name'.
     */
    @Value("${gcp.cloud.storage.base-directory-name}")
    private String baseDirectoryName;

    private final Storage storage;
    private static final DocumentMapper documentMapper = DocumentMapper.MAPPER;


    @Override
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Uploads a file to Google Cloud Storage and returns a Document object with metadata.
     * <p>
     * Steps performed:
     * <ol>
     *   <li>Builds the full file path using the base directory and a timestamped filename</li>
     *   <li>Creates a BlobInfo object with content type and file path</li>
     *   <li>Uploads the file stream to GCS</li>
     *   <li>Logs the upload process</li>
     *   <li>Returns a Document object with relevant metadata</li>
     * </ol>
     * If an error occurs, a StorageException is thrown and logged.
     *
     * @param file the file to upload
     * @return Document object containing metadata about the uploaded file
     * @throws StorageException if the upload fails
     */
    @Override
    public Document uploadFile(File file) {
        final MultipartFile multipartFile = file.getMultipartFile();
        final String fullFilePath = baseDirectoryName +
                DocumentFileNameUtils.getFileNameWithTimestamp(multipartFile.getOriginalFilename());
        log.info("Uploading file to GCS: {}", fullFilePath);
        final BlobId blobId = BlobId.of(bucketName, fullFilePath);
        final String contentType = multipartFile.getContentType();
        final BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        try (InputStream inputStream = multipartFile.getInputStream()) {
            // Upload the file to GCS
            storage.createFrom(blobInfo, inputStream);
            log.info("File uploaded successfully to GCS: {}", fullFilePath);
            final OffsetDateTime uploadTime = OffsetDateTime.now();

            // Create and return a Document object with the uploaded file information
            return documentMapper.fileToNewDocument(
                    file,
                    uploadTime,
                    fullFilePath);
        } catch (Exception e) {
            log.error("Failed to upload file to GCS: {}", fullFilePath, e);
            throw StorageException.with(multipartFile.getOriginalFilename()).get();
        }
    }

    @Override
    public String downloadFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.error("File path is null or empty");
            return null;
        }
        final Blob blob = storage.get(BlobId.of(bucketName, filePath));
        if(blob == null || !blob.exists()) {
            log.error("File not found in GCS: {}", filePath);
            return null;
        }
        return Base64.getEncoder().encodeToString(blob.getContent());
    }
}
