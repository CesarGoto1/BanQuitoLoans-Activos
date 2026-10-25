package com.grupob.bancobanquito.loans.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.loans.DisbursementPreview;
import com.grupob.bancobanquito.loans.DisbursementResult;
import com.grupob.bancobanquito.loans.LoanService;
import com.grupob.bancobanquito.loans.EstadoCartera;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simulated")
@Transactional
class LoanServiceTest {

    @Autowired
    LoanService loanService;

    @Autowired
    LoanRepository loanRepository;

    @Autowired
    InstallmentRepository installmentRepository;

    @Test
    void previewCalculatesInsuranceSolcaAndNetAmount() {
        DisbursementPreview preview = loanService.previewDisbursement(9001);

        assertThat(preview.monthlyInsurance()).isEqualByComparingTo("6.00");
        assertThat(preview.solcaContribution()).isEqualByComparingTo("50.00");
        assertThat(preview.netAmount()).isEqualByComparingTo("9950.00");
        assertThat(preview.accounts()).isNotEmpty();
    }

    @Test
    void disbursesAndSavesTheLoanWithItsSchedule() {
        DisbursementResult result = loanService.disburse(9002, "2200000002");

        assertThat(result.loanNumber()).isEqualTo("OP-20261031-009002");
        assertThat(result.netAmount()).isEqualByComparingTo("4975.00");

        Loan loan = loanRepository.findByLoanNumber(result.loanNumber()).orElseThrow();
        assertThat(loan.getPortfolioStatus()).isEqualTo(EstadoCartera.POR_VENCER);
        assertThat(loan.getDisbursementInstructionStatus()).isEqualTo(DisbursementInstructionStatus.DELIVERED);
        assertThat(installmentRepository.countByLoanId(loan.getId())).isEqualTo(12);

        BigDecimal totalPrincipal = installmentRepository.findAll().stream()
                .filter(installment -> installment.getLoan().getId().equals(loan.getId()))
                .map(Installment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipal).isEqualByComparingTo(loan.getApprovedAmount());
    }

    @Test
    void doesNotDisburseTwice() {
        loanService.disburse(9001, "2200000001");

        assertThatThrownBy(() -> loanService.disburse(9001, "2200000001"))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(loanService.findDisbursement(9001)).isPresent();
    }

    @Test
    void doesNotDisburseAnApplicationThatIsNotApproved() {
        assertThatThrownBy(() -> loanService.disburse(1234, "2200000001"))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void doesNotDisburseToAnAccountOfAnotherCustomer() {
        assertThatThrownBy(() -> loanService.disburse(9001, "2200000002"))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void leavesNothingSavedWhenTheLiabilitiesCoreFails() {
        assertThatThrownBy(() -> loanService.disburse(9001, "9999999999"))
                .isInstanceOf(LoanException.class)
                .extracting(e -> ((LoanException) e).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);

        assertThat(loanRepository.existsByApplicationId(9001)).isFalse();
    }
}
