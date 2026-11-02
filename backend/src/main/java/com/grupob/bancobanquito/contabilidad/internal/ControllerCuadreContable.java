package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.ResultadoCuadreContable;
import com.grupob.bancobanquito.contabilidad.ServiceCuadreContable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analista/cuadres-contables")
@Tag(name = "Cuadre contable")
class ControllerCuadreContable {

    private final ServiceCuadreContable servicioCuadre;

    ControllerCuadreContable(ServiceCuadreContable servicioCuadre) {
        this.servicioCuadre = servicioCuadre;
    }

    @GetMapping
    @Operation(summary = "Consulta el cuadre aritmético de los asientos de una fecha de negocio")
    ResultadoCuadreContable consultar(
            @RequestParam("fechaNegocio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNegocio) {
        return servicioCuadre.consultarCuadre(fechaNegocio);
    }
}
