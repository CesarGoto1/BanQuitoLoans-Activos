package com.grupob.bancobanquito.origination.internal;

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
@RequestMapping("/api/loan-applications")
@Tag(name = "Loan applications")
class LoanApplicationController {

    private final CustomerApplicationService customerApplicationService;

    LoanApplicationController(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registers a customer's loan application and evaluates the payment capacity with the reference rate")
    LoanApplicationResponse register(@RequestHeader("X-Identificacion") String nationalId,
                                     @Valid @RequestBody LoanApplicationRequest request) {
        return LoanApplicationResponse.from(customerApplicationService.register(
                customerApplicationService.customerIdOf(nationalId), request));
    }

    @GetMapping
    @Operation(summary = "Loan applications of one customer, optionally filtered by status, one page at a time")
    Page<LoanApplicationResponse> findByCustomer(
            @RequestHeader("X-Identificacion") String nationalId,
            @RequestParam(required = false) LoanApplicationStatus status,
            @PageableDefault(size = 10, sort = "registeredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return customerApplicationService.findByCustomer(customerApplicationService.customerIdOf(nationalId), status,
                        pageable)
                .map(LoanApplicationResponse::from);
    }
}
