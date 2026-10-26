package com.grupob.bancobanquito.acceso.internal;

import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sesiones-analista")
class ControllerInicioSesionAnalista {

    private final ServiceAnalista analystService;

    ControllerInicioSesionAnalista(ServiceAnalista analystService) {
        this.analystService = analystService;
    }

    @PostMapping
    SesionAnalista login(@Valid @RequestBody PeticionInicioSesionAnalista request) {
        return analystService.login(request.cedula());
    }
}
