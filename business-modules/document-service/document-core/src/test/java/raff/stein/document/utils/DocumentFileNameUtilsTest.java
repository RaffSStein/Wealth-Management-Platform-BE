package raff.stein.document.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentFileNameUtilsTest {

    @Test
    void shouldPrefixFileNameWithUtcTimestampWithoutColons() {
        final Instant before = Instant.now();

        final String fileName = DocumentFileNameUtils.getFileNameWithTimestamp("id-card.pdf");

        final String[] parts = fileName.split("_", 2);
        assertEquals("id-card.pdf", parts[1], fileName);
        assertFalse(parts[0].contains(":"), fileName);
        // Prefix is the instant with ':' replaced by '-', e.g. 2026-11-07T15-09-55.123Z
        final Instant timestamp = Instant.parse(parts[0].replaceFirst("T(\\d{2})-(\\d{2})-", "T$1:$2:"));
        assertFalse(timestamp.isBefore(before.minusSeconds(1)), fileName);
    }

    @Test
    void shouldReturnNullForNullFileName() {
        assertNull(DocumentFileNameUtils.getFileNameWithTimestamp(null));
    }
}
