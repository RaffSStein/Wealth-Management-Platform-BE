package raff.stein.document.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import raff.stein.document.model.Document;
import raff.stein.document.model.File;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("!gcs & !s3")
public class DefaultStorageService implements CloudStorageService {

    // This is a default implementation of CloudStorageService that does not perform any actual storage operations.
    @Override
    public String getBucketName() {
        return "";
    }

    @Override
    public Document uploadFile(File file) {
        return null;
    }

    @Override
    public String downloadFile(String filePath) {
        return null;
    }
}
