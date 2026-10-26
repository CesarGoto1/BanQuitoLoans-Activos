package com.grupob.bancobanquito.originacion.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulaciones-prestamo")
@Tag(name = "Loan simulations")
class ControllerSimulacionPrestamo {

    private final ServiceSolicitudCliente customerApplicationService;

    ControllerSimulacionPrestamo(ServiceSolicitudCliente customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @PostMapping
    @Operation(summary = "Simulates a loan with the reference rate and returns its projected schedule without saving anything")
    SimulacionPrestamo simulate(@Valid @RequestBody PeticionSimulacionPrestamo request) {
        return customerApplicationService.simulate(request);
    }
}
