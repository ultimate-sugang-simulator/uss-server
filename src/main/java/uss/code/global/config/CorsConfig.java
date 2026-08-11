package uss.code.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    private static final String ALL_PATH_PATTERN = "/**";
    private static final String ALL_HEADER_PATTERN = "*";
    private static final long MAX_AGE = 3600L;

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://uss.inuappcenter.kr",
            "https://ultimate-sugang-web.inuappcenter.kr",
            "https://ultimate-sugang-web.pages.dev"
    );

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH");

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.addAllowedHeader(ALL_HEADER_PATTERN);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(MAX_AGE);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ALL_PATH_PATTERN, configuration);

        return source;
    }
}
