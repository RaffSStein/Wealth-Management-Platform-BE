package raff.stein.document.service.validation;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Slf4j
public class FileValidationResult {

    private final List<ValidationError> errors = new ArrayList<>();
    private OffsetDateTime validatedAt;

    public void addError(String validator, String message) {
        errors.add(new ValidationError(validator, message));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void logErrors() {
        if (!isValid()) {
            errors.forEach(error -> log.error("Validation error in [{}]: {}", error.validator, error.message));
        } else {
            log.info("File validation passed with no errors.");
        }
    }

    public void finalizeValidation() {
        this.validatedAt = OffsetDateTime.now();
    }


    public record ValidationError(String validator, String message) {}
}
