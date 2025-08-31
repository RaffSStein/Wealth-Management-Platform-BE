package raff.stein.customer.client.eu.sanctions.model.screen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request payload for the AML/Sanctions screening service used by the Feign client.
 * This model carries the customer identity data, contextual metadata and screening options
 * (lists/providers, matching strategy, thresholds) required by an external screening engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScreenRequest {

    // IMPORTANT: This is a mock implementation. Replace with real sanction screening logic.


    // Identity
    /** Primary/legal first name of the subject to screen. */
    @NotBlank
    @Size(max = 100)
    private String firstName;

    /** Primary/legal last name of the subject to screen. */
    @NotBlank
    @Size(max = 100)
    private String lastName;

    /** Optional middle name(s) or patronymic. */
    @Size(max = 100)
    private String middleName;

    /** Date of birth (ISO-8601). */
    @NotNull
    private LocalDate dateOfBirth;

    /** City or locality of birth. */
    @Size(max = 120)
    private String placeOfBirth;

    /** ISO 3166-1 alpha-2 nationality code (e.g., IT, GB). */
    @NotBlank
    @Pattern(regexp = "[A-Z]{2}")
    private String nationality;

    /** Gender of the subject if available (free text or controlled values). */
    @Size(max = 30)
    private String gender;

    /** Known aliases for the subject (alternative names). */
    private List<@Size(max = 200) String> aliases;

    // Identifiers
    /** Internal customer identifier in the bank core systems. */
    @Size(max = 64)
    private String customerInternalId;

    /** National ID number where applicable. */
    @Size(max = 64)
    private String nationalIdNumber;

    /** Passport number where applicable. */
    @Size(max = 64)
    private String passportNumber;

    /** Tax identification number (e.g., TIN/CF). */
    @Size(max = 64)
    private String taxId;

    // Address
    @Size(max = 150)
    private String addressLine1;

    @Size(max = 150)
    private String addressLine2;

    @Size(max = 80)
    private String city;

    @Size(max = 20)
    private String postalCode;

    /** ISO 3166-1 alpha-2 country code of residency address. */
    @Pattern(regexp = "[A-Z]{2}")
    private String countryCode;

    // Context / Risk
    /** Politically Exposed Person flag if already known. */
    private Boolean pep;

    /** PEP roles or functions, if any. */
    private List<@Size(max = 120) String> pepRoles;

    /** Countries associated to residency, operations or travel relevant for screening. */
    private List<@Pattern(regexp = "[A-Z]{2}") String> relatedCountries;

    // ---- Screening options ----
    /** Which screening scenario is being executed. */
    @NotNull
    private ScreeningType screeningType;

    /** Matching strategy used by the screening engine. */
    @NotNull
    private MatchMode matchMode;

    /** Minimum score/threshold for a candidate match (0-100). */
    @Min(0)
    @Max(100)
    private Integer minMatchScore;

    /** Which lists to query (e.g., UN, EU, OFAC, PEP). */
    private List<ListType> listTypes;

    /** Screening data providers/vendors to query. */
    private List<@Size(max = 80) String> providers;

    // ---- Technical metadata ----
    /** Correlation ID propagated across services. */
    @Size(max = 100)
    private String correlationId;

    /** Unique id for this screening request. */
    @Size(max = 100)
    private String requestId;

    /** Username, service-account or client id performing the request. */
    @Size(max = 120)
    private String requestedBy;

    /** Calling system identifier (e.g., CUSTOMER-SERVICE). */
    @Size(max = 80)
    private String sourceSystem;

    /** Client-side timestamp of the request. */
    @Builder.Default
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    /** Free-form notes to support manual review. */
    @Size(max = 1000)
    private String notes;

    // ---- Nested enums kept local to avoid scattering types across modules ----

    /** Screening scenario to help downstream routing and policies. */
    public enum ScreeningType {
        @JsonProperty("CUSTOMER_ONBOARDING") CUSTOMER_ONBOARDING,
        @JsonProperty("CUSTOMER_PERIODIC_REVIEW") CUSTOMER_PERIODIC_REVIEW,
        @JsonProperty("TRANSACTION") TRANSACTION,
        @JsonProperty("MANUAL_CHECK") MANUAL_CHECK
    }

    /** Matching strategy requested to the engine. */
    public enum MatchMode {
        @JsonProperty("EXACT") EXACT,
        @JsonProperty("FUZZY") FUZZY,
        @JsonProperty("PHONETIC") PHONETIC
    }

    /** Well-known list families. */
    public enum ListType {
        @JsonProperty("UN") UN,
        @JsonProperty("EU") EU,
        @JsonProperty("OFAC") OFAC,
        @JsonProperty("HMT") HMT,
        @JsonProperty("LOCAL") LOCAL,
        @JsonProperty("PEP") PEP,
        @JsonProperty("WATCHLIST") WATCHLIST
    }
}
