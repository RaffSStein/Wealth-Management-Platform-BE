package raff.stein.customer.client.eu.sanctions.model.screen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response payload returned by the AML/Sanctions screening engine.
 * Contains high-level outcome, aggregate scores, decisioning info and
 * the detailed list of candidate matches produced by the screening.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScreenResponse {

    // IMPORTANT: This is a mock implementation. Replace with real sanction screening logic.

    // Correlation / audit
    /** Correlation id echoed back by the engine. */
    private String correlationId;
    /** Request id echoed back by the engine. */
    private String requestId;
    /** When the screening was completed server-side. */
    private OffsetDateTime screenedAt;
    /** Engine/build version that processed the request. */
    private String engineVersion;

    // Overall status / decision
    /** Processing status (technical). */
    private Status status;
    /** Business decision derived from matches/rules. */
    private Decision decision;
    /** Rules that contributed to the decision (if any). */
    private List<String> rulesTriggered;

    // Aggregates
    /** True if at least one candidate match has been found. */
    private Boolean matchFound;
    /** Highest score among candidate matches (0-100). */
    private Integer maxMatchScore;
    /** Total number of candidate matches. */
    private Integer matchesCount;

    // Details
    /** Candidate matches returned by the engine. */
    private List<Match> matches;

    // Diagnostics
    /** Non-fatal warnings produced during processing. */
    private List<String> warnings;
    /** Fatal errors if the engine failed to process the request. */
    private List<String> errors;

    // Nested types

    public enum Status {
        @JsonProperty("SUCCESS") SUCCESS,
        @JsonProperty("PARTIAL") PARTIAL,
        @JsonProperty("FAILED") FAILED
    }

    public enum Decision {
        @JsonProperty("CLEAR") CLEAR,
        @JsonProperty("REVIEW") REVIEW,
        @JsonProperty("HIT") HIT
    }

    /** One candidate match as returned by the screening provider. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Match {
        /** Provider-specific identifier for this match. */
        private String matchId;
        /** The list family that produced the hit (e.g., EU, OFAC, PEP). */
        private ScreenRequest.ListType listType;
        /** Data provider/vendor that produced the match. */
        private String provider;
        /** External entity id on the source list, if available. */
        private String entityExternalId;
        /** Name as listed on the sanctions/PEP source. */
        private String entityName;
        /** Also-known-as names associated to the entity. */
        private List<String> akaNames;
        /** Match score as computed by the engine (0-100). */
        private Integer score;
        /** Matching strategy used to produce this candidate. */
        private ScreenRequest.MatchMode matchMode;
        /** True if flagged as PEP. */
        private Boolean pep;
        /** True if sanctioned (present on a sanctions list). */
        private Boolean sanction;
        /** True if adverse media evidence exists. */
        private Boolean adverseMedia;
        /** References to list/regime details supporting the match. */
        private List<SanctionReference> references;
        /** Optional free-form notes/justification. */
        private String notes;
    }

    /** Reference to a sanctions/regulatory source entry. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SanctionReference {
        /** Regime owning the list (e.g., EU, UN, OFAC). */
        private String regime;
        /** Friendly list name (e.g., EU Consolidated). */
        private String listName;
        /** Source reference identifier/code. */
        private String referenceId;
        /** When the entity was first listed. */
        private OffsetDateTime listedAt;
        /** Last update timestamp of the list entry. */
        private OffsetDateTime lastUpdatedAt;
    }
}
