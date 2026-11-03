package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ArchivoRecaudacionSolicitado;
import com.grupob.bancobanquito.cobranza.ServiceSolicitudArchivoRecaudacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analista/archivos-recaudacion")
@Tag(name = "Collections file")
class ControllerArchivoRecaudacion {

    private final ServiceSolicitudArchivoRecaudacion servicioSolicitud;

    ControllerArchivoRecaudacion(ServiceSolicitudArchivoRecaudacion servicioSolicitud) {
        this.servicioSolicitud = servicioSolicitud;
    }

    @PostMapping
    @Operation(summary = "Asks the Liabilities Core to generate the collections file of the current business date")
    ArchivoRecaudacionSolicitado solicitar() {
        return servicioSolicitud.solicitarArchivoDeLaFechaVigente();
    }
}
