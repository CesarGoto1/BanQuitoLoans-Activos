package com.grupob.bancobanquito.origination.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credit-limits")
@Tag(name = "Credit limits")
class CreditLimitsController {

    private final CustomerApplicationService customerApplicationService;

    CreditLimitsController(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @GetMapping
    @Operation(summary = "Returns the amount and term ranges in force on the business date")
    CreditLimits findCurrent() {
        return customerApplicationService.findCreditLimits();
    }
}
