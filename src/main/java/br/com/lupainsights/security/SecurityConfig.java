package br.com.lupainsights.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticatedRateLimitFilter authenticatedRateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          AuthenticatedRateLimitFilter authenticatedRateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticatedRateLimitFilter = authenticatedRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login",
                                "/auth/forgot-password", "/auth/reset-password", "/auth/bootstrap-admin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/analytics/event").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health", "/actuator/health", "/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/plans", "/plans/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cnpj/preview", "/cnpj/preview/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/payments/mercadopago/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payments/mercadopago/webhook").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"erro\":\"Não autenticado\"}");
                }))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authenticatedRateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
