package uss.code.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import uss.code.auth.filter.AdmissionTokenFilter;
import uss.code.auth.filter.JwtAuthenticationFilter;
import uss.code.auth.filter.AuthenticationExceptionFilter;
import uss.code.auth.infra.AtProvider;
import uss.code.auth.infra.JwtProvider;

@Configuration
@RequiredArgsConstructor
public class FilterChainConfig {

    private static final int JWT_EXCEPTION_FILTER_ORDER = 1;
    private static final int JWT_AUTHENTICATION_FILTER_ORDER = 2;
    private static final int ADMISSION_TOKEN_FILTER_ORDER = 3;

    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final AtProvider atProvider;

    @Bean
    public FilterRegistrationBean<AuthenticationExceptionFilter> jwtExceptionFilter() {
        FilterRegistrationBean<AuthenticationExceptionFilter> jwtExceptionFilterBean = new FilterRegistrationBean<>();

        jwtExceptionFilterBean.setFilter(new AuthenticationExceptionFilter(objectMapper));
        jwtExceptionFilterBean.setOrder(JWT_EXCEPTION_FILTER_ORDER);

        return jwtExceptionFilterBean;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterBean = new FilterRegistrationBean<>();

        jwtAuthenticationFilterBean.setFilter(new JwtAuthenticationFilter(jwtProvider));
        jwtAuthenticationFilterBean.setOrder(JWT_AUTHENTICATION_FILTER_ORDER);

        return jwtAuthenticationFilterBean;
    }

    @Bean
    public FilterRegistrationBean<AdmissionTokenFilter> admissionTokenFilter(){
        FilterRegistrationBean<AdmissionTokenFilter> admissionTokenFilterBean = new FilterRegistrationBean<>();

        admissionTokenFilterBean.setFilter(new AdmissionTokenFilter(atProvider));
        admissionTokenFilterBean.setOrder(ADMISSION_TOKEN_FILTER_ORDER);

        return admissionTokenFilterBean;
    }
}
