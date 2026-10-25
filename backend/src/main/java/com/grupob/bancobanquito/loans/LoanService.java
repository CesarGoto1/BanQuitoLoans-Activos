package com.grupob.bancobanquito.loans;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoanService {

    DisbursementPreview previewDisbursement(Integer applicationId);

    DisbursementResult disburse(Integer applicationId, String destinationAccount);

    Optional<DisbursementResult> findDisbursement(Integer applicationId);

    Page<LoanSummary> findByCustomer(String customerNationalId, PortfolioStatus portfolioStatus, Pageable pageable);

    Page<InstallmentSummary> findInstallments(String loanNumber, Pageable pageable);
}
