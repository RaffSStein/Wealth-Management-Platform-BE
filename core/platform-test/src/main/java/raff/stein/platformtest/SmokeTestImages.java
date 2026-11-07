package raff.stein.platformtest;

/**
 * Container images used by the smoke tests, pinned so that a build is reproducible.
 * Keep them close to the versions in docker-compose.yml.
 */
public final class SmokeTestImages {

    public static final String POSTGRES = "postgres:15-alpine";
    public static final String KAFKA = "apache/kafka-native:3.9.1";
    public static final String REDIS = "redis:7-alpine";

    private SmokeTestImages() {
    }
}
