package com.grupob.bancobanquito.originacion.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes-credito")
@Tag(name = "Loan applications")
class ControllerSolicitud {

    private final ServiceSolicitudCliente customerApplicationService;

    ControllerSolicitud(ServiceSolicitudCliente customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registers a customer's loan application and evaluates the payment capacity with the reference rate")
    RespuestaSolicitud register(@RequestHeader("X-Identificacion") String cedula,
                                     @Valid @RequestBody PeticionSolicitud request) {
        return RespuestaSolicitud.from(customerApplicationService.register(
                customerApplicationService.customerIdOf(cedula), request));
    }

    @GetMapping
    @Operation(summary = "Loan applications of one customer, optionally filtered by status, one page at a time")
    Page<RespuestaSolicitud> findByCustomer(
            @RequestHeader("X-Identificacion") String cedula,
            @RequestParam(name = "estado", required = false) EstadoSolicitud estado,
            @PageableDefault(size = 10, sort = "fechaHoraRegistro", direction = Sort.Direction.DESC) Pageable paginacion) {
        return customerApplicationService.findByCustomer(customerApplicationService.customerIdOf(cedula), estado,
                        paginacion)
                .map(RespuestaSolicitud::from);
    }
}
