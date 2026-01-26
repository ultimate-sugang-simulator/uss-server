package uss.code.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import uss.code.auth.filter.JwtAuthenticationFilter;
import uss.code.auth.filter.JwtExceptionFilter;
import uss.code.auth.infra.JwtProvider;

@Configuration
@RequiredArgsConstructor
public class FilterChainConfig {

    private static final int JWT_EXCEPTION_FILTER_ORDER = 1;
    private static final int JWT_AUTHENTICATION_FILTER_ORDER = 2;

    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;

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
