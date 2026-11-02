package com.grupob.bancobanquito.reportes.internal;

import com.grupob.bancobanquito.reportes.ResultadoComposicionCartera;
import com.grupob.bancobanquito.reportes.ResultadoMorosidad;
import com.grupob.bancobanquito.reportes.ResultadoRollRate;
import com.grupob.bancobanquito.reportes.ResultadoSaludCartera;
import com.grupob.bancobanquito.reportes.ServiceReportesCartera;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analista/reportes")
@Tag(name = "Reportes de cartera")
class ControllerReportesCartera {

    private final ServiceReportesCartera servicio;

    ControllerReportesCartera(ServiceReportesCartera servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/morosidad")
    @Operation(summary = "Consulta la morosidad a una fecha de corte")
    ResultadoMorosidad morosidad(@RequestParam("fechaCorte") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.consultarMorosidad(fecha);
    }

    @GetMapping({"/composicion", "/provisiones"})
    @Operation(summary = "Consulta saldos y provisiones constituidas por tramo")
    ResultadoComposicionCartera composicion(@RequestParam("fechaCorte") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.consultarComposicion(fecha);
    }

    @GetMapping("/salud")
    @Operation(summary = "Consulta salud, concentración y distribuciones de cartera")
    ResultadoSaludCartera salud(@RequestParam("fechaCorte") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.consultarSalud(fecha);
    }

    @GetMapping("/roll-rate")
    @Operation(summary = "Compara dos fotografías confirmadas mediante sus saldos iniciales")
    ResultadoRollRate rollRate(
            @RequestParam("fechaInicial") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicial,
            @RequestParam("fechaFinal") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return servicio.consultarRollRate(inicial, fin);
    }
}
