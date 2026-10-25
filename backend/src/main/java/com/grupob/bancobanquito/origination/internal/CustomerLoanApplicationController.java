package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/loan-applications")
class CustomerLoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    CustomerLoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LoanApplicationResponse register(@RequestHeader("X-Identificacion") String identification,
                                     @Valid @RequestBody RegisterLoanApplicationRequest request) {
        if (!identification.matches("\\d{10}")) {
            throw new LoanApplicationExceptions.InvalidIdentification();
        }
        return loanApplicationService.register(identification, request);
    }
}
