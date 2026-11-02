package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.ResultadoProvisiones;
import com.grupob.bancobanquito.contabilidad.ServiceProvisiones;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provisiones")
class ControllerProvisiones {

    private final ServiceProvisiones service;

    ControllerProvisiones(ServiceProvisiones service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ResultadoProvisiones> calculate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNegocio) {
        return ResponseEntity.ok(service.calcular(fechaNegocio));
    }
}
