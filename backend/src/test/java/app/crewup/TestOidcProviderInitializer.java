package app.crewup;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Points the backend's {@code issuer-uri} at {@link TestOidcProvider}. An initializer (rather than a dynamic property)
 * because Boot chooses its token decoder from this property while it reads the configuration.
 */
class TestOidcProviderInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of("spring.security.oauth2.resourceserver.jwt.issuer-uri="
                        + TestOidcProvider.instance().issuer())
                .applyTo(context);
    }
}
