package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.loans.InstallmentSummary;
import com.grupob.bancobanquito.loans.LoanInquiryService;
import com.grupob.bancobanquito.loans.LoanSummary;
import com.grupob.bancobanquito.loans.PortfolioStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loans")
class LoanController {

    private final LoanInquiryService loanInquiryService;

    LoanController(LoanInquiryService loanInquiryService) {
        this.loanInquiryService = loanInquiryService;
    }

    @GetMapping("/{loanNumber}")
    @Operation(summary = "Loan found by its loan number, with its portfolio status, days past due and principal balance")
    LoanSummary findByLoanNumber(@PathVariable String loanNumber) {
        return loanInquiryService.findByLoanNumber(loanNumber);
    }

    @GetMapping
    @Operation(summary = "Loans of one customer, filtered by national ID and optionally by portfolio status, one page at a time")
    Page<LoanSummary> findByCustomer(
            @RequestParam @Pattern(regexp = "\\d{10}", message = "La cédula debe tener 10 dígitos.") String customerNationalId,
            @RequestParam(required = false) PortfolioStatus portfolioStatus,
            @PageableDefault(size = 10, sort = "disbursementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return loanInquiryService.findByCustomer(customerNationalId, portfolioStatus, pageable);
    }

    @GetMapping("/{loanNumber}/installments")
    @Operation(summary = "Amortization schedule of a loan, one page at a time")
    Page<InstallmentSummary> findInstallments(
            @PathVariable String loanNumber,
            @PageableDefault(size = 12, sort = "installmentNumber") Pageable pageable) {
        return loanInquiryService.findInstallments(loanNumber, pageable);
    }
}
