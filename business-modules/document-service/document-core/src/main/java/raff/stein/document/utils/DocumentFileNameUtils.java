package raff.stein.document.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
public final class DocumentFileNameUtils {

    private DocumentFileNameUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Generates a file name with a timestamp prefix based on the current time.
     *
     * @param originalFileName the original file name to which the timestamp will be prepended
     * @return a new file name with the current timestamp prepended, or null if the original file name is null
     */
    public static String getFileNameWithTimestamp(String originalFileName) {
        if (originalFileName == null) {
            return null;
        }
        final OffsetDateTime offsetDateTime = OffsetDateTime.now();
        final String timestamp = offsetDateTime.toInstant().toString().replace(":", "-");
        return timestamp + "_" + originalFileName;
    }
}
