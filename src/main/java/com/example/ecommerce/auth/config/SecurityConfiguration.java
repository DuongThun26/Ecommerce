package com.example.ecommerce.auth.config;

import com.example.ecommerce.auth.service.UserDetailServiceCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JWTDecoderConfiguration jwtDecoderConfiguration;
    private final UserDetailServiceCustom userDetailServiceCustom;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure JWT -> GrantedAuthority conversion using `roles` claim
        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null || roles.isEmpty()) {
                return List.of();
            }
            return roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
        });

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        // 1. PUBLIC ENDPOINTS (Swagger, Auth, Products Catalog, VNPay Callbacks)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users", "/customers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/categories/{id}", "/categories/code/**").permitAll()
                        .requestMatchers("/payments/vnpay/return", "/payments/vnpay/return/**").permitAll()
                        .requestMatchers("/payments/vnpay/ipn", "/payments/vnpay/ipn/**").permitAll()

                        .requestMatchers("/auth/logout").hasAnyRole("ADMIN", "CUSTOMER", "PRODUCT_OWNER")
                        
                        // 1.1 CATEGORIES MANAGEMENT
                        .requestMatchers(HttpMethod.GET, "/categories/my-categories").hasAnyRole("ADMIN", "PRODUCT_OWNER")
                        .requestMatchers(HttpMethod.POST, "/categories", "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories", "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories", "/categories/**").hasRole("ADMIN")
                        
                        // 2. CUSTOMER Self / Profile: CUSTOMER can view their own orders and invoices
                        .requestMatchers(HttpMethod.GET, "/customers/me/**", "/customers/invoices", "/customers/orders").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/customers/*/orders", "/customers/*/invoices").hasAnyRole("ADMIN", "CUSTOMER")
                        
                        // 3. CUSTOMERS Management: ADMIN only
                        .requestMatchers("/customers", "/customers/**").hasRole("ADMIN")
                        
                        // 4. USER PROFILE: Self-management for all authenticated users
                        .requestMatchers(HttpMethod.GET, "/users/me").hasAnyRole("ADMIN", "CUSTOMER", "PRODUCT_OWNER")
                        .requestMatchers(HttpMethod.PUT, "/users/me", "/users/me/**").hasAnyRole("ADMIN", "CUSTOMER", "PRODUCT_OWNER")
                        
                        // 4.1 USERS & ROLES Management: ADMIN only
                        .requestMatchers("/users", "/users/**").hasRole("ADMIN")
                        
                        // 5. PURCHASE TICKETS: PO & ADMIN
                        .requestMatchers(HttpMethod.POST, "/tickets/**").hasAnyRole("PRODUCT_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/tickets/**").hasAnyRole("PRODUCT_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/tickets/**").hasAnyRole("PRODUCT_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/tickets", "/tickets/**").hasAnyRole("PRODUCT_OWNER", "ADMIN", "CUSTOMER")
                        
                        // 6. INVOICES Management: ADMIN only
                        .requestMatchers(HttpMethod.POST, "/invoices/orders/*/generate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/invoices", "/invoices/").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/invoices/**", "/orders/*/invoice").hasAnyRole("ADMIN", "CUSTOMER")
                        
                        // 7. ORDER CREATION & ITEMS MANAGEMENT
                        .requestMatchers(
                                "/orders/*/items", "/orders/*/items/**",
                                "/api/orders/*/items", "/api/orders/*/items/**",
                                "/api/orders", "/api/orders/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/orders", "/orders/draft", "/orders/*/submit").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/orders/*").hasAnyRole("CUSTOMER", "ADMIN")
                        
                        // 8. ORDER MANAGEMENT:
                        // Danh sách tất cả đơn hàng: ADMIN & PRODUCT_OWNER
                        .requestMatchers(HttpMethod.GET, "/orders", "/orders/").hasAnyRole("ADMIN", "PRODUCT_OWNER")
                        // Chi tiết đơn hàng và lịch sử đơn hàng: ADMIN, PRODUCT_OWNER & CUSTOMER
                        .requestMatchers(HttpMethod.GET, "/orders/{id}", "/orders/{id}/history", "/orders/{id}/**").hasAnyRole("ADMIN", "PRODUCT_OWNER", "CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/orders/**").hasAnyRole("ADMIN", "PRODUCT_OWNER")
                        .requestMatchers(HttpMethod.POST, "/orders/{id}/cancel").hasAnyRole("ADMIN", "CUSTOMER")

                        // 9. PRODUCTS MANAGEMENT: ADMIN & PRODUCT_OWNER
                        .requestMatchers(HttpMethod.POST, "/products", "/products/**").hasAnyRole("ADMIN", "PRODUCT_OWNER")
                        .requestMatchers(HttpMethod.PUT, "/products", "/products/**").hasAnyRole("ADMIN", "PRODUCT_OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/products", "/products/**").hasAnyRole("ADMIN", "PRODUCT_OWNER")

                        // 10. CLOUDINARY IMAGE UPLOADS: ADMIN, PRODUCT_OWNER & CUSTOMER
                        .requestMatchers("/cloudinary", "/cloudinary/**").hasAnyRole("ADMIN", "PRODUCT_OWNER", "CUSTOMER")
                        
                        // 11. PAYMENTS: INITIATE FOR CUSTOMER ONLY
                        .requestMatchers(HttpMethod.POST, "/payments/vnpay").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/payments", "/payments/**").hasAnyRole("ADMIN", "CUSTOMER", "PRODUCT_OWNER")
                        
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoderConfiguration).jwtAuthenticationConverter(jwtAuthConverter))
                );
        return http.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailServiceCustom);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
