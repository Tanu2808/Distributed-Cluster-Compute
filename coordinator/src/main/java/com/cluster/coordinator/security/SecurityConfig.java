package com.cluster.coordinator.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cluster.security.api-username:admin}")
    private String username;

    @Value("${cluster.security.api-password:admin_secret}")
    private String password;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, org.springframework.security.authentication.AuthenticationProvider authProvider) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/info", "/ws/cluster", "/api/cluster/enroll").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authProvider)
            .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationProvider authenticationProvider(com.cluster.coordinator.repository.WorkerRepository workerRepository) {
        return new org.springframework.security.authentication.AuthenticationProvider() {
            @Override
            public org.springframework.security.core.Authentication authenticate(org.springframework.security.core.Authentication authentication) throws org.springframework.security.core.AuthenticationException {
                String name = authentication.getName();
                String credentials = authentication.getCredentials().toString();

                // Admin auth
                if (username.equals(name) && password.equals(credentials)) {
                    java.util.List<org.springframework.security.core.GrantedAuthority> authorities = java.util.List.of(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_WORKER"));
                    return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(name, credentials, authorities);
                }

                // Enrolled worker auth
                if (!username.equals(name)) {
                    com.cluster.coordinator.model.Worker worker = workerRepository.findById(name).orElse(null);
                    if (worker != null && worker.getRuntimeCredentialHash() != null) {
                        String providedHash = hashCredential(credentials);
                        if (worker.getRuntimeCredentialHash().equals(providedHash)) {
                            java.util.List<org.springframework.security.core.GrantedAuthority> authorities = java.util.List.of(
                                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_WORKER"));
                            return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(name, credentials, authorities);
                        }
                    }
                }

                throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
            
            private String hashCredential(String raw) {
                try {
                    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                    byte[] encodedhash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    return java.util.Base64.getEncoder().encodeToString(encodedhash);
                } catch (java.security.NoSuchAlgorithmException e) {
                    throw new RuntimeException("SHA-256 not found", e);
                }
            }
        };
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
