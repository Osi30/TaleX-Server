package com.talex.server.configs;

import com.talex.server.annotations.CurrentAccountId;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.env:local}")
    private String appEnv;

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentAccountId.class);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Talex Server API")
                        .version("1.0.0")
                        .description("Tài liệu hướng dẫn sử dụng API hệ thống Talex"))
                // Áp dụng cơ chế bảo mật này cho TOÀN BỘ các API hiển thị trên Swagger
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));

        if (!"local".equals(appEnv)) {
            openAPI.addServersItem(new Server().url("https://api.talex.pro.vn").description("Production"));
        }
        openAPI.addServersItem(new Server().url("http://localhost:8080").description("Local"));

        return openAPI;
    }
}