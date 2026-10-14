package app.crewup;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary, wide-open security for the M0 scaffold (T3), so the health endpoint is reachable without Spring
 * Security's default basic-auth login. T5 replaces this with the real resource-server configuration.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }
}
