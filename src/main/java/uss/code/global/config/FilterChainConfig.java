package uss.code.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import uss.code.auth.filter.JwtAuthenticationFilter;
import uss.code.auth.filter.JwtExceptionFilter;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.filter.HttpLoggingFilter;

@Configuration
@RequiredArgsConstructor
public class FilterChainConfig {

    private static final int HTTP_LOGGING_FILTER_ORDER = 0;
    private static final int JWT_EXCEPTION_FILTER_ORDER = 1;
    private static final int JWT_AUTHENTICATION_FILTER_ORDER = 2;

    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;

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
