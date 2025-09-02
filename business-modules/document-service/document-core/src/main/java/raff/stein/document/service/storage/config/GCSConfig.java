package raff.stein.document.service.storage.config;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class GCSConfig {

    @Value("${gcp.cloud.storage.project-id}")
    private String projectId;

    @Value("${gcp.cloud.storage.bucket-name}")
    private String bucketName;

    @Value("${gcp.cloud.storage.host}")
    private String host;


    /**
     * This method initializes the GCS Storage service with the provided project ID, bucket name, and host.
     * It is meant to be used in production or other environments where GCS is not local.
     */
    @Bean
    @Profile("!gcs & !local & !s3")
    public Storage getStorage() {
        log.info("Initializing GCS Storage with projectId: {}, bucketName: {}, host: {}", projectId, bucketName, host);
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setHost(host)
                .build()
                .getService();
    }

    /**
     * This method initializes the GCS Storage service with the provided project ID, bucket name, and host.
     * It is meant to be used in local development environments where GCS is emulated.
     */
    @Bean
    @Profile("gcs")
    public Storage getLocalGcsStorage() {
        log.info("Initializing Local GCS Storage with projectId: {}, bucketName: {}, host: {}", projectId, bucketName, host);
        StorageOptions storageOptions = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setHost(host)
                .build();
        Storage storage = storageOptions.getService();
        log.info("Local GCS Storage initialized successfully.");
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            log.info("Creating bucket: {}", bucketName);
            bucket = storage.create(BucketInfo.newBuilder(bucketName).build());
            log.info("Bucket {} created successfully.", bucketName);
        }
        return storage;
    }
}
