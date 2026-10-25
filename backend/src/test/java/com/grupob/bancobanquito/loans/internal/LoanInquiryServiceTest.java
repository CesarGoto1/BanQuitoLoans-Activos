package com.grupob.bancobanquito.loans.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.loans.InstallmentSummary;
import com.grupob.bancobanquito.loans.LoanInquiryService;
import com.grupob.bancobanquito.loans.LoanService;
import com.grupob.bancobanquito.loans.LoanSummary;
import com.grupob.bancobanquito.loans.PortfolioStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LoanInquiryServiceTest {

    @Autowired
    LoanService loanService;

    @Autowired
    LoanInquiryService loanInquiryService;

    @Test
    void findsALoanByItsLoanNumber() {
        loanService.disburse(9002, "2200000002");

        LoanSummary loan = loanInquiryService.findByLoanNumber("OP-20261031-009002");

        assertThat(loan.customerNationalId()).isEqualTo("1712345675");
        assertThat(loan.approvedAmount()).isEqualByComparingTo("5000.00");
        assertThat(loan.principalBalance()).isEqualByComparingTo("5000.00");
        assertThat(loan.daysPastDue()).isZero();
        assertThat(loan.portfolioStatus()).isEqualTo(PortfolioStatus.CURRENT);
    }

    @Test
    void reportsNotFoundForAnUnknownLoanNumber() {
        assertThatThrownBy(() -> loanInquiryService.findByLoanNumber("OP-20261031-999999"))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findsLoansOfOneCustomerOnePageAtATime() {
        loanService.disburse(9003, "2200000003");

        Page<LoanSummary> loans = loanInquiryService.findByCustomer("1709876543", null, PageRequest.of(0, 10));
        assertThat(loans.getContent()).extracting(LoanSummary::loanNumber).containsExactly("OP-20261031-009003");

        Page<InstallmentSummary> installments = loanInquiryService.findInstallments("OP-20261031-009003",
                PageRequest.of(0, 12));
        assertThat(installments.getContent()).hasSize(12);
        assertThat(installments.getTotalElements()).isEqualTo(60);
    }

    @Test
    void filtersLoansOfOneCustomerByPortfolioStatus() {
        loanService.disburse(9003, "2200000003");

        assertThat(loanInquiryService.findByCustomer("1709876543", PortfolioStatus.CURRENT, PageRequest.of(0, 10))
                .getContent()).hasSize(1);
        assertThat(loanInquiryService.findByCustomer("1709876543", PortfolioStatus.PAST_DUE, PageRequest.of(0, 10))
                .getContent()).isEmpty();
    }

    @Test
    void reportsNotFoundForTheScheduleOfAnUnknownLoan() {
        assertThatThrownBy(() -> loanInquiryService.findInstallments("OP-20261031-999999", PageRequest.of(0, 12)))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
