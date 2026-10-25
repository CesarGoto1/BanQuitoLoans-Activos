package com.grupob.bancobanquito.loans;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoanInquiryService {

    LoanSummary findByLoanNumber(String loanNumber);

    Page<LoanSummary> findByCustomer(String customerNationalId, PortfolioStatus portfolioStatus, Pageable pageable);

    Page<InstallmentSummary> findInstallments(String loanNumber, Pageable pageable);
}