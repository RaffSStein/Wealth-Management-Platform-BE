package raff.stein.platformtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the whole service against real Postgres, Kafka and Redis containers and checks that it reports UP.
 * Unit tests don't catch a dependency update that breaks bean wiring, entity mapping or serializers at startup; this does.
 * <p>
 * Skipped locally when Docker is not available, so a build without Docker still passes. In CI it always runs and
 * fails if Docker is missing: a silent skip there would turn a broken update green.
 * The containers are started once per JVM and shared by every smoke test in the module.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf("raff.stein.platformtest.AbstractServiceSmokeTest#shouldRun")
public abstract class AbstractServiceSmokeTest {

    private static final int REDIS_PORT = 6379;
    // Set to "true" by GitHub Actions (and most CI systems)
    private static final String CI_ENV_VARIABLE = "CI";

    @SuppressWarnings("resource") // stopped by Testcontainers' Ryuk when the JVM exits
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(SmokeTestImages.POSTGRES);
    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA = new KafkaContainer(SmokeTestImages.KAFKA);
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new GenericContainer<>(SmokeTestImages.REDIS).withExposedPorts(REDIS_PORT);

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        // Started here and not with @Container: @DynamicPropertySource runs before the JUnit extension would start them
        startContainers();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Kafka is configured through the platform's own key, not spring.kafka.bootstrap-servers (see KafkaConfiguration)
        registry.add("kafka.broker.connectionUri", KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
    }

    static boolean shouldRun() {
        return Boolean.parseBoolean(System.getenv(CI_ENV_VARIABLE)) || DockerClientFactory.instance().isDockerAvailable();
    }

    private static synchronized void startContainers() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!KAFKA.isRunning()) {
            KAFKA.start();
        }
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    @Test
    void shouldStartAndReportHealthUp() {
        final ResponseEntity<String> response = RestClient.create("http://localhost:" + port + contextPath)
                .get()
                .uri("/actuator/health")
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""), "Health is not UP: " + response.getBody());
    }
}
