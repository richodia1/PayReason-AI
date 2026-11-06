package ai.payreason.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService users(@Value("${payreason.security.username}") String username,
                            @Value("${payreason.security.password}") String password) {
        if (username.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Set an administrator username and a password of at least 12 characters.");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password)).roles("ADMIN").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/paypal/webhooks"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/auth/csrf", "/api/auth/login",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error", "/api/paypal/webhooks").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> response.sendError(401, "Invalid credentials")))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .deleteCookies("PAYREASON_SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(401, "Sign in required")))
                .build();
    }
}
