package com.grupob.bancobanquito.access.internal.config;
import com.grupob.bancobanquito.access.internal.config.*;
import com.grupob.bancobanquito.access.internal.cors.*;
import com.grupob.bancobanquito.access.internal.security.*;
import com.grupob.bancobanquito.access.internal.exceptions.*;
import com.grupob.bancobanquito.access.Identity;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorActual", modifyOnCreation = false)
public class AuditoriaConfig {

    @Bean
    AuditorAware<String> auditorActual(AccessServiceImpl accesoService) {
        return () -> Optional.of(accesoService.identidadSiExiste().map(Identity::identificacion).orElse("sistema"));
    }
}
















