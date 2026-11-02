package com.grupob.bancobanquito.cierre.internal;

import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.time.LocalDate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
class ProgramacionCierre {

    private final ServiceCierreDiaImpl closingService;
    private final ServiceFechaNegocio businessDateService;

    ProgramacionCierre(ServiceCierreDiaImpl closingService, ServiceFechaNegocio businessDateService) {
        this.closingService = closingService;
        this.businessDateService = businessDateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    void recoverInterruptedClosings() {
        closingService.recuperarCierresInterrumpidos();
    }

    @Scheduled(cron = "${cierre.programacion:-}", zone = "America/Guayaquil")
    void runScheduledClosing() {
        LocalDate fechaNegocio = businessDateService.getCurrentBusinessDate();
        try {
            closingService.ejecutarCierre(fechaNegocio, ServiceCierreDiaImpl.USUARIO_SISTEMA);
        } catch (CierreException e) {
        }
    }
}
