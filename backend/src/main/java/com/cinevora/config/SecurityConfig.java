package com.cinevora.config;

import com.cinevora.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<com.cinevora.security.AbuseProtectionFilter> abuseRegistration(com.cinevora.security.AbuseProtectionFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter); registration.setEnabled(false); return registration;
    }
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter); registration.setEnabled(false); return registration;
    }
    @Bean PasswordEncoder passwordEncoder() { return new com.cinevora.security.BoundedBCryptPasswordEncoder(); }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter, com.cinevora.security.AbuseProtectionFilter abuseFilter, @Value("${app.auth.cookie-secure:false}") boolean cookieSecure) throws Exception {
        var repository = new org.springframework.security.web.csrf.CookieCsrfTokenRepository();
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).sameSite("Lax").path("/api/v1/auth").secure(cookieSecure));
        return http.csrf(csrf -> csrf.csrfTokenRepository(repository)
                        .requireCsrfProtectionMatcher(request -> request.getRequestURI().startsWith("/api/v1/auth/")
                                && !java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()))
                        .withObjectPostProcessor(new org.springframework.security.config.ObjectPostProcessor<org.springframework.security.web.csrf.CsrfFilter>() {
                            @Override public <O extends org.springframework.security.web.csrf.CsrfFilter> O postProcess(O filter) {
                                filter.setAccessDeniedHandler((request, response, exception) -> {
                                    response.setStatus(403); response.setContentType("application/json");
                                    response.getWriter().write("{\"success\":false,\"message\":\"CSRF validation failed\",\"data\":null}");
                                }); return filter;
                            }
                        }))
                .cors(cors -> {})
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\",\"data\":null}");
                }))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health", "/media/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/movies/**", "/api/v1/categories/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(abuseFilter, org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin-patterns:http://localhost:5173}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Profile-Id", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
