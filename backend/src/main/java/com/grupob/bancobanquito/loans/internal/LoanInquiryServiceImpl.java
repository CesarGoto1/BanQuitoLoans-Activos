package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.loans.InstallmentSummary;
import com.grupob.bancobanquito.loans.LoanInquiryService;
import com.grupob.bancobanquito.loans.LoanSummary;
import com.grupob.bancobanquito.loans.PortfolioStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class LoanInquiryServiceImpl implements LoanInquiryService {

    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;

    LoanInquiryServiceImpl(LoanRepository loanRepository, InstallmentRepository installmentRepository) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
    }

    @Override
    public LoanSummary findByLoanNumber(String loanNumber) {
        return summary(existingLoan(loanNumber));
    }

    @Override
    public Page<LoanSummary> findByCustomer(String customerNationalId, PortfolioStatus portfolioStatus,
                                            Pageable pageable) {
        Page<Loan> loans = portfolioStatus == null
                ? loanRepository.findByCustomerNationalId(customerNationalId, pageable)
                : loanRepository.findByCustomerNationalIdAndPortfolioStatus(customerNationalId, portfolioStatus, pageable);
        return loans.map(this::summary);
    }

    @Override
    public Page<InstallmentSummary> findInstallments(String loanNumber, Pageable pageable) {
        return installmentRepository.findByLoanId(existingLoan(loanNumber).getId(), pageable)
                .map(this::summary);
    }

    private Loan existingLoan(String loanNumber) {
        return loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new LoanException(HttpStatus.NOT_FOUND, "La operación " + loanNumber + " no existe."));
    }

    private LoanSummary summary(Loan loan) {
        return new LoanSummary(loan.getLoanNumber(), loan.getApplicationId(), loan.getCustomerNationalId(),
                loan.getApprovedAmount(), loan.getEffectiveAnnualRate(), loan.getTermMonths(),
                loan.getAmortizationMethod(), loan.getDisbursementDate(), loan.getPrincipalBalance(),
                loan.getDaysPastDue(), loan.getPortfolioStatus());
    }

    private InstallmentSummary summary(Installment installment) {
        return new InstallmentSummary(installment.getInstallmentNumber(), installment.getDueDate(),
                installment.getPrincipalAmount(), installment.getInterestAmount(), installment.getInsuranceAmount(),
                installment.getTotalAmount(), installment.getRemainingPrincipalBalance(),
                installment.getStatus().name());
    }
}
