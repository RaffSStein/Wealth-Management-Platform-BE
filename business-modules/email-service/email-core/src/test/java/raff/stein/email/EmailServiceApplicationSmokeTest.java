package raff.stein.email;

import org.springframework.test.context.TestPropertySource;
import raff.stein.platformtest.AbstractServiceSmokeTest;

// No SMTP server runs in the test, so the mail health check would report DOWN
@TestPropertySource(properties = "management.health.mail.enabled=false")
class EmailServiceApplicationSmokeTest extends AbstractServiceSmokeTest {
}
