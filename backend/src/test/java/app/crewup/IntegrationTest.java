package app.crewup;

import app.crewup.shared.ApiErrorProbeController;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Full application context against Testcontainers PostgreSQL and the {@link TestOidcProvider}, with MockMvc. Every
 * integration test uses this same annotation so Spring caches one context (and one container) for the whole test run.
 *
 * <p>Logs are JSON (as outside local development), so log tests read fields instead of matching text. The setting
 * lives here, not on a single test class: a second context in the same JVM keeps the first one's logging setup.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "logging.structured.format.console=ecs")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ApiProbeController.class, ApiErrorProbeController.class})
@ContextConfiguration(initializers = TestOidcProviderInitializer.class)
public @interface IntegrationTest {}
