package raff.stein.customer.service.aml.utils;

import com.ibm.icu.util.Region;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * Utility class to determine a jurisdiction (USA, UK, EU, OTHER) from an ISO 3166-1 alpha-2 country code,
 * leveraging ICU4J/CLDR data (no hardcoded country lists).
 */
@Slf4j
public final class JurisdictionUtils {

    private JurisdictionUtils() {
        // Utility class
    }

    /** Jurisdictions supported by the platform. */
    public enum Jurisdiction {
        USA,
        UK,
        EU,
        OTHER
    }

    // Cached regions
    private static final Region EU = Region.getInstance("EU");
    private static final Region US = Region.getInstance("US");
    private static final Region GB = Region.getInstance("GB"); // "UK" is an alias for GB in many contexts


    /**
     * Resolves the jurisdiction from a given ISO 3166-1 alpha-2 country code.
     * Uses ICU4J Region containment:
     * - US -> USA
     * - GB/UK -> UK
     * - Any EU member state -> EU
     * - Otherwise -> OTHER
     *
     * @param countryCode ISO 3166-1 alpha-2 code (e.g., "IT", "US", "GB", "UK").
     * @return The resolved Jurisdiction enum.
     */
    public static Jurisdiction resolveJurisdiction(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return Jurisdiction.OTHER;
        }
        final String normalizedCountryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        try {
            // Normalize UK alias
            Region country = Region.getInstance("UK".equals(normalizedCountryCode) ? "GB" : normalizedCountryCode);

            if (country.equals(US))
                return Jurisdiction.USA;
            if (country.equals(GB))
                return Jurisdiction.UK;
            if (EU.contains(country))
                return Jurisdiction.EU;
        } catch (IllegalArgumentException ex) {
            // Unknown/non-standard code -> OTHER
            return Jurisdiction.OTHER;
        }
        return Jurisdiction.OTHER;
    }
}
