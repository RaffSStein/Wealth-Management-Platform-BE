package raff.stein.document.service.storage;

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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * Service implementation for Amazon S3 integration.
 * <p>
 * This service provides methods to upload files to an S3 bucket and expose bucket information.
 * It is activated only when the Spring profile "s3" is enabled.
 * <p>
 * Main responsibilities:
 * <ul>
 *   <li>Upload files to a configured S3 bucket</li>
 *   <li>Return metadata about the uploaded document</li>
 *   <li>Handle exceptions and logging during the upload process</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile("s3")
public class S3StorageService implements CloudStorageService {

    /**
     * The name of the S3 bucket where files will be stored.
     * Configured via the property 'spring.cloud.aws.bucket-name'.
     */
    @Value("${spring.cloud.aws.bucket-name}")
    private String bucketName;

    /**
     * The base directory inside the bucket where files will be uploaded.
     * Configured via the property 'spring.cloud.aws.base-directory-name'.
     */
    @Value("${spring.cloud.aws.base-directory-name}")
    private String baseDirectoryName;

    /**
     * AWS SDK v2 S3 client used to interact with Amazon S3.
     * Provided via constructor injection.
     */
    private final S3Client s3Client;

    /**
     * MapStruct mapper used to convert a File (upload request) into a new Document domain model
     * enriched with storage metadata (upload time, storage path, etc.).
     */
    private static final DocumentMapper documentMapper = DocumentMapper.MAPPER;


    /**
     * Returns the configured S3 bucket name.
     *
     * @return the S3 bucket name
     */
    @Override
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Uploads a file to Amazon S3 and returns a Document object with metadata.
     * <p>
     * Steps performed:
     * <ol>
     *   <li>Build the full file path using the base directory and a timestamped filename</li>
     *   <li>Create a {@link PutObjectRequest} with bucket, key and content type</li>
     *   <li>Upload the stream to S3 via {@link S3Client#putObject}</li>
     *   <li>Log the upload process</li>
     *   <li>Return a Document object with relevant metadata</li>
     * </ol>
     * If an error occurs, a {@link StorageException} is thrown and logged.
     *
     * @param file the file wrapper containing the {@link MultipartFile} to upload
     * @return Document object containing metadata about the uploaded file
     * @throws StorageException if the upload fails
     */
    @Override
    public Document uploadFile(File file) {
        final MultipartFile multipartFile = file.getMultipartFile();
        final String fullFilePath = baseDirectoryName +
                DocumentFileNameUtils.getFileNameWithTimestamp(multipartFile.getOriginalFilename());
        log.info("Uploading file to S3: {}", fullFilePath);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .contentType(multipartFile.getContentType())
                .key(fullFilePath)
                .build();
        try (InputStream inputStream = multipartFile.getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
            final OffsetDateTime uploadTime = OffsetDateTime.now();
            return documentMapper.fileToNewDocument(
                    file,
                    uploadTime,
                    fullFilePath);

        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", fullFilePath, e);
            throw new StorageException(null, e.getMessage());
        }
    }

    /**
     * Downloads a file from Amazon S3.
     * <p>
     * Not implemented yet. Returning {@code null} by design placeholder.
     *
     * @param filePath the S3 object key
     * @return base64-encoded content of the file, or {@code null} if not implemented
     */
    @Override
    public String downloadFile(String filePath) {
        return null;
    }
}
