package com.grupob.bancobanquito.access.internal.config;
import com.grupob.bancobanquito.access.internal.config.*;
import com.grupob.bancobanquito.access.internal.cors.*;
import com.grupob.bancobanquito.access.internal.security.*;
import com.grupob.bancobanquito.access.internal.exceptions.*;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApiCorePrestamos() {
        return new OpenAPI()
                .info(new Info().title("BanQuito · Core de Préstamos").version("v1"))
                .components(new Components()
                        .addSecuritySchemes("X-Identification", esquemaCabecera("X-Identification"))
                        .addSecuritySchemes("X-Role", esquemaCabecera("X-Role")))
                .addSecurityItem(new SecurityRequirement().addList("X-Identification").addList("X-Role"));
    }

    private SecurityScheme esquemaCabecera(String nombre) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(nombre);
    }
}
















