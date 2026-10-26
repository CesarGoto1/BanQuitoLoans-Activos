package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.prestamos.VistaPreviaDesembolso;
import com.grupob.bancobanquito.prestamos.ResultadoDesembolso;
import com.grupob.bancobanquito.prestamos.ServicePrestamo;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
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
    ServicePrestamo loanService;

    @Autowired
    PrestamoRepository loanRepository;

    @Autowired
    CuotaRepository installmentRepository;

    @Test
    void previewCalculatesInsuranceSolcaAndNetAmount() {
        VistaPreviaDesembolso preview = loanService.previewDisbursement(9001);

        assertThat(preview.seguroMensual()).isEqualByComparingTo("6.00");
        assertThat(preview.contribucionSolca()).isEqualByComparingTo("50.00");
        assertThat(preview.montoNeto()).isEqualByComparingTo("9950.00");
        assertThat(preview.cuentas()).isNotEmpty();
    }

    @Test
    void disbursesAndSavesTheLoanWithItsSchedule() {
        ResultadoDesembolso result = loanService.disburse(9002, "2200000002");

        assertThat(result.numeroOperacion()).isEqualTo("OP-20261031-009002");
        assertThat(result.montoNeto()).isEqualByComparingTo("4975.00");

        Prestamo loan = loanRepository.findByNumeroOperacion(result.numeroOperacion()).orElseThrow();
        assertThat(loan.getPortfolioStatus()).isEqualTo(EstadoCartera.POR_VENCER);
        assertThat(loan.getDisbursementInstructionStatus()).isEqualTo(EstadoInstruccionDesembolso.ENTREGADA);
        assertThat(installmentRepository.countByLoanId(loan.getId())).isEqualTo(12);

        BigDecimal totalPrincipal = installmentRepository.findAll().stream()
                .filter(installment -> installment.getLoan().getId().equals(loan.getId()))
                .map(Cuota::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipal).isEqualByComparingTo(loan.getApprovedAmount());
    }

    @Test
    void doesNotDisburseTwice() {
        loanService.disburse(9001, "2200000001");

        assertThatThrownBy(() -> loanService.disburse(9001, "2200000001"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(loanService.findDisbursement(9001)).isPresent();
    }

    @Test
    void doesNotDisburseAnApplicationThatIsNotApproved() {
        assertThatThrownBy(() -> loanService.disburse(1234, "2200000001"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void doesNotDisburseToAnAccountOfAnotherCustomer() {
        assertThatThrownBy(() -> loanService.disburse(9001, "2200000002"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void leavesNothingSavedWhenTheLiabilitiesCoreFails() {
        assertThatThrownBy(() -> loanService.disburse(9001, "9999999999"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);

        assertThat(loanRepository.existsByIdSolicitud(9001)).isFalse();
    }
}
