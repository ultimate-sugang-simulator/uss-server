package uss.code.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String ACCESS_TOKEN_KEY = "access-token";
    private static final String REFRESH_TOKEN_KEY = "refresh-token";

    @Bean
    public OpenAPI openAPI() {
        final SecurityScheme accessTokenSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(ACCESS_TOKEN_KEY);

        final SecurityScheme refreshTokenSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(REFRESH_TOKEN_KEY);

        final Components components = new Components()
                .addSecuritySchemes(ACCESS_TOKEN_KEY, accessTokenSecurityScheme)
                .addSecuritySchemes(REFRESH_TOKEN_KEY, refreshTokenSecurityScheme);

        final SecurityRequirement requirement = new SecurityRequirement()
                .addList(ACCESS_TOKEN_KEY)
                .addList(REFRESH_TOKEN_KEY);

        return new OpenAPI()
                .components(components)
                .info(apiInfo())
                .addSecurityItem(requirement)
                .servers(servers());
    }

    private Info apiInfo() {
        return new Info()
                .title("USS API Docs")
                .description("궁극의 수강신청 시뮬레이터 API 명세서")
                .version("1.0.0");
    }

    private List<Server> servers() {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("http://localhost:8080").description("Dev env"));
        return servers;
    }
}