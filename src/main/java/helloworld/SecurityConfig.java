package helloworld;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// Keeps the demo page public now that Spring Security is on the classpath.
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(sessions -> sessions.requireExplicitAuthenticationStrategy(false));
        http.securityContext(sc -> sc.requireExplicitSave(false));
        http.authorizeHttpRequests().anyRequest().permitAll();
        return http.build();
    }
}
