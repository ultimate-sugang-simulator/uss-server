package uss.code.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.ObjectMapper;
import uss.code.auth.filter.JwtAuthenticationFilter;
import uss.code.auth.filter.JwtExceptionFilter;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.filter.HttpLoggingFilter;

@Configuration
@RequiredArgsConstructor
public class FilterChainConfig {

    private static final int CORS_FILTER_ORDER = 0;
    private static final int HTTP_LOGGING_FILTER_ORDER = 1;
    private static final int JWT_EXCEPTION_FILTER_ORDER = 2;
    private static final int JWT_AUTHENTICATION_FILTER_ORDER = 3;

    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new CorsFilter(corsConfigurationSource));
        bean.setOrder(CORS_FILTER_ORDER);

        return bean;
    }

    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilter() {
        FilterRegistrationBean<HttpLoggingFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new HttpLoggingFilter());
        bean.setOrder(HTTP_LOGGING_FILTER_ORDER);

        return bean;
    }

    @Bean
    public FilterRegistrationBean<JwtExceptionFilter> jwtExceptionFilter() {
        FilterRegistrationBean<JwtExceptionFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new JwtExceptionFilter(objectMapper));
        bean.setOrder(JWT_EXCEPTION_FILTER_ORDER);

        return bean;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new JwtAuthenticationFilter(jwtProvider));
        bean.setOrder(JWT_AUTHENTICATION_FILTER_ORDER);

        return bean;
    }
}
