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
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.time.OffsetDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("s3")
public class S3StorageService implements CloudStorageService {

    @Value("${spring.cloud.aws.bucket-name}")
    private String bucketName;

    @Value("${spring.cloud.aws.base-directory-name}")
    private String baseDirectoryName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    private static final DocumentMapper documentMapper = DocumentMapper.MAPPER;


    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public Document uploadFile(File file) {
        final MultipartFile multipartFile = file.getMultipartFile();
        final String fullFilePath = baseDirectoryName +
                DocumentFileNameUtils.getFileNameWithTimestamp(multipartFile.getOriginalFilename());
        log.info("Uploading file to S3: {}", fullFilePath);

        S3Client s3 = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .contentType(multipartFile.getContentType())
                .key(fullFilePath)
                .build();
        try (InputStream inputStream = multipartFile.getInputStream()) {
            s3.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
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

    @Override
    public String downloadFile(String filePath) {
        return null;
    }
}
