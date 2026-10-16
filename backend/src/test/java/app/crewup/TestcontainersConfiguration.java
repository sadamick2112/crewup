package app.crewup;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL for integration tests (TEST-4), on the same image as docker-compose.yml. {@code @ServiceConnection}
 * points the datasource at the container, so tests never touch the Compose database. The container is one instance
 * per test run, so a test class that needs its own Spring context (for example, different logging settings) doesn't
 * start a second database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:18.6"));

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
