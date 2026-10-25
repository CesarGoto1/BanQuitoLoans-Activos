package com.grupob.bancobanquito.access.internal.cors;
import com.grupob.bancobanquito.access.internal.config.*;
import com.grupob.bancobanquito.access.internal.cors.*;
import com.grupob.bancobanquito.access.internal.security.*;
import com.grupob.bancobanquito.access.internal.exceptions.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AccessWebConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public AccessWebConfig(@Value("${cors.origenes}") String origenes) {
        this.origenesPermitidos = origenes.trim().split("\\s*,\\s*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new IdentificationInterceptor())
                .addPathPatterns("/api/client/**", "/api/analyst/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
















