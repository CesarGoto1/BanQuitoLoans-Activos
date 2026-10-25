package com.grupob.bancobanquito.origination.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loan-simulations")
@Tag(name = "Loan simulations")
class LoanSimulationController {

    private final CustomerApplicationService customerApplicationService;

    LoanSimulationController(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @PostMapping
    @Operation(summary = "Simulates a loan with the reference rate and returns its projected schedule without saving anything")
    LoanSimulation simulate(@Valid @RequestBody LoanSimulationRequest request) {
        return customerApplicationService.simulate(request);
    }
}
