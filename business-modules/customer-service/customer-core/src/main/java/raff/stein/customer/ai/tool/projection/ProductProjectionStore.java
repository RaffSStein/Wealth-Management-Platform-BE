package raff.stein.customer.ai.tool.projection;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory product projection.
 * <p>
 * In production this would be fed by product-service events.
 */
@Component
public class ProductProjectionStore {

    public record ProductSummary(String isin, String name, String riskLevel) {}

    private final Map<String, ProductSummary> productsByIsin = new ConcurrentHashMap<>();

    public ProductProjectionStore() {
        // Seed with a couple of examples for local development.
        productsByIsin.put("IT0000000001", new ProductSummary("IT0000000001", "Example Bond Fund", "MEDIUM"));
        productsByIsin.put("IE0000000002", new ProductSummary("IE0000000002", "Example Equity Fund", "HIGH"));
    }

    public Optional<ProductSummary> findByIsin(String isin) {
        if (isin == null || isin.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(productsByIsin.get(isin.trim().toUpperCase()));
    }
}

