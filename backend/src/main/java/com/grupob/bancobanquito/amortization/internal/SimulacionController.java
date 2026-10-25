package com.grupob.bancobanquito.amortization.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente/simulaciones")
@Tag(name = "Simulación", description = "RF-01 y RF-07. Cálculo referencial sin persistencia.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Formato inválido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "401", description = "Falta identificación o rol", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "403", description = "Rol no autorizado", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "409", description = "Monto o plazo fuera del rango vigente", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "503", description = "Configuración o base de datos no disponible", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
class SimulacionController {

    private final SimulacionService simulacionService;

    SimulacionController(SimulacionService simulacionService) {
        this.simulacionService = simulacionService;
    }

    @GetMapping("/parametros")
    @Operation(summary = "Consultar condiciones vigentes de simulación")
    ResponseEntity<ParametrosSimulacionResponse> consultarParametros(
            @Parameter(description = "Identificación ficticia de 10 dígitos", required = true, example = "1700000000")
            @RequestHeader(value = "X-Identificacion", required = false) String identificacion,
            @Parameter(description = "Rol del acceso simulado", required = true, example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        validarAcceso(identificacion, rol);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(simulacionService.consultarParametros());
    }

    @PostMapping
    @Operation(summary = "Simular un préstamo francés o alemán", description = "La TEA, el seguro, SOLCA y la fecha se obtienen del servidor. No crea solicitudes ni operaciones. Tasas expresadas como fracciones; importes en USD.")
    ResponseEntity<SimulacionResponse> simular(
            @Parameter(description = "Identificación ficticia de 10 dígitos", required = true, example = "1700000000")
            @RequestHeader(value = "X-Identificacion", required = false) String identificacion,
            @Parameter(description = "Rol del acceso simulado", required = true, example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @Valid @RequestBody SimulacionRequest solicitud) {
        validarAcceso(identificacion, rol);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(simulacionService.simular(solicitud));
    }

    private void validarAcceso(String identificacion, String rol) {
        if (identificacion == null || identificacion.isBlank() || rol == null || rol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Indica la identificación y el rol del cliente.");
        }
        if (!"CLIENTE".equals(rol)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La simulación está disponible para el rol CLIENTE.");
        }
        if (!identificacion.matches("[0-9]{10}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La identificación debe contener 10 dígitos.");
        }
    }
}
