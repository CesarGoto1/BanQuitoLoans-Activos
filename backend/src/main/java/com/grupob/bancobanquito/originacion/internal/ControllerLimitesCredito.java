package com.grupob.bancobanquito.originacion.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/limites-credito")
@Tag(name = "Credit limits")
class ControllerLimitesCredito {

    private final ServiceSolicitudCliente customerApplicationService;

    ControllerLimitesCredito(ServiceSolicitudCliente customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @GetMapping
    @Operation(summary = "Returns the amount and term ranges in force on the business date")
    LimitesCredito findCurrent() {
        return customerApplicationService.findCreditLimits();
    }
}
