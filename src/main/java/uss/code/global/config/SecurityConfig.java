package uss.code.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uss.code.auth.infra.BCryptPasswordEncoder;
import uss.code.auth.infra.PasswordEncoder;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
