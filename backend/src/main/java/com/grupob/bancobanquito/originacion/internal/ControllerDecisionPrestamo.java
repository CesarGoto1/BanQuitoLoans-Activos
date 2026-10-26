package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.originacion.ResultadoDecisionPrestamo;
import com.grupob.bancobanquito.originacion.ServiceDecisionPrestamo;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analista/solicitudes")
class ControllerDecisionPrestamo {

    private final ServiceDecisionPrestamo decisionService;

    ControllerDecisionPrestamo(ServiceDecisionPrestamo decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping("/{idSolicitud}/aprobacion")
    ResponseEntity<ResultadoDecisionPrestamo> approveApplication(@PathVariable("idSolicitud") Integer idSolicitud,
            @RequestHeader(name = "X-Rol", required = false) String role,
            @RequestHeader(name = "X-Identificacion", required = false) String identification,
            @Valid @RequestBody PeticionAprobarPrestamo request) {
        validateHeaders(role, identification);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(decisionService.approveApplication(idSolicitud, identification.trim(), request.teaAsignada()));
    }

    @PostMapping("/{idSolicitud}/rechazo")
    ResponseEntity<ResultadoDecisionPrestamo> rejectApplication(@PathVariable("idSolicitud") Integer idSolicitud,
            @RequestHeader(name = "X-Rol", required = false) String role,
            @RequestHeader(name = "X-Identificacion", required = false) String identification,
            @Valid @RequestBody PeticionRechazarPrestamo request) {
        validateHeaders(role, identification);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(decisionService.rejectApplication(idSolicitud, identification.trim(), request.motivoRechazo()));
    }

    private void validateHeaders(String role, String identification) {
        if (role == null || !"ANALISTA".equalsIgnoreCase(role.trim())) {
            throw new AccesoDenegadoAnalistaException("Acceso denegado: se requiere la cabecera X-Rol con valor ANALISTA.");
        }
        if (identification == null || !identification.trim().matches("[0-9]{10}")) {
            throw new CabeceraFaltanteException("La cabecera X-Identificacion debe contener una identificación de 10 dígitos.");
        }
    }
}
