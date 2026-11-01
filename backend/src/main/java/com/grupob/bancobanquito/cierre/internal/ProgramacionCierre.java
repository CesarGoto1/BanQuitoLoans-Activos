package com.grupob.bancobanquito.cierre.internal;

import com.grupob.bancobanquito.cierre.ResultadoCierreDia;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// El cierre programado está apagado por defecto ("-"); se activa con la propiedad cierre.programacion,
// por ejemplo CIERRE_PROGRAMACION="0 0 23 * * *" para cerrar todos los días a las 23:00.
@Component
@EnableScheduling
class ProgramacionCierre {

    private static final Logger log = LoggerFactory.getLogger(ProgramacionCierre.class);

    private final ServiceCierreDiaImpl closingService;
    private final ServiceFechaNegocio businessDateService;

    ProgramacionCierre(ServiceCierreDiaImpl closingService, ServiceFechaNegocio businessDateService) {
        this.closingService = closingService;
        this.businessDateService = businessDateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    void recoverInterruptedClosings() {
        int recuperados = closingService.recuperarCierresInterrumpidos();
        if (recuperados > 0) {
            log.warn("{} cierre(s) quedaron interrumpidos y pasaron a FALLIDO; se pueden reintentar", recuperados);
        }
    }

    @Scheduled(cron = "${cierre.programacion:-}", zone = "America/Guayaquil")
    void runScheduledClosing() {
        LocalDate fechaNegocio = businessDateService.getCurrentBusinessDate();
        try {
            ResultadoCierreDia resultado = closingService.ejecutarCierre(fechaNegocio, ServiceCierreDiaImpl.USUARIO_SISTEMA);
            log.info("Cierre programado del {}: {}", fechaNegocio, resultado.estadoCierre());
        } catch (ExcepcionCierre e) {
            log.warn("Cierre programado del {} no ejecutado: {}", fechaNegocio, e.getMessage());
        }
    }
}
