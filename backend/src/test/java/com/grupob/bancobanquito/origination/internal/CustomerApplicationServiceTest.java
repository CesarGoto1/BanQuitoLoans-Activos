package com.grupob.bancobanquito.origination.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import com.grupob.bancobanquito.origination.LoanApplicationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simulated")
@Transactional
class CustomerApplicationServiceTest {

    @Autowired
    CustomerApplicationService customerApplicationService;

    @Autowired
    LoanApplicationService loanApplicationService;

    @Test
    void registersAnApplicationThatPassesTheEvaluation() {
        LoanApplication application = customerApplicationService.register(request(1, "10000.00", 12, AmortizationMethod.FRENCH));

        assertThat(application.getStatus()).isEqualTo(LoanApplicationStatus.REGISTERED);
        assertThat(application.getReferenceInstallment()).isLessThan(new BigDecimal("920.00"));
        assertThat(application.getResolutionDate()).isNull();
        assertThat(application.getRejectionReason()).isNull();
    }

    @Test
    void rejectsAGermanApplicationWhoseFirstInstallmentExceedsTheLimit() {
        LoanApplication application = customerApplicationService.register(request(1, "10000.00", 12, AmortizationMethod.GERMAN));

        assertThat(application.getStatus()).isEqualTo(LoanApplicationStatus.REJECTED);
        assertThat(application.getReferenceInstallment()).isGreaterThan(new BigDecimal("920.00"));
        assertThat(application.getRejectionReason()).contains("920,00");
        assertThat(application.getResolutionDate()).isNotNull();
    }

    @Test
    void doesNotRegisterAnAmountOutOfRange() {
        assertThatThrownBy(() -> customerApplicationService.register(request(1, "40000.00", 12, AmortizationMethod.FRENCH)))
                .isInstanceOf(OriginationException.class)
                .hasMessageContaining("30.000,00")
                .extracting(e -> ((OriginationException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void doesNotRegisterASecondPendingApplication() {
        customerApplicationService.register(request(2, "5000.00", 24, AmortizationMethod.FRENCH));

        assertThatThrownBy(() -> customerApplicationService.register(request(2, "3000.00", 12, AmortizationMethod.FRENCH)))
                .isInstanceOf(OriginationException.class)
                .hasMessageContaining("pendiente");
    }

    @Test
    @Sql(statements = """
            INSERT INTO "operaciones" ("idSolicitud", "idCliente", "cedulaCliente", "sistemaAmortizacion", "numeroOperacion",
                "montoAprobado", "teaAsignada", "plazoMeses", "seguroMensual", "contribucionSolca", "montoNeto",
                "cuentaDestino", "fechaDesembolso", "estadoInstruccionDesembolso", "estadoCartera", "saldoCapital",
                "diasMora", "usuarioCreacion")
            VALUES (9003, 3, '1709876543', 'FRANCES', 'OP-PRUEBA-MORA', 30000.00, 0.1550, 60, 18.00, 150.00, 29850.00,
                '2200000003', DATE '2026-10-31', 'ENTREGADA', 'VENCIDA', 30000.00, 5, 'pruebas');
            """)
    void doesNotRegisterWhenTheCustomerHasDaysPastDue() {
        assertThatThrownBy(() -> customerApplicationService.register(request(3, "1000.00", 12, AmortizationMethod.FRENCH)))
                .isInstanceOf(OriginationException.class)
                .hasMessageContaining("mora");
    }

    @Test
    void doesNotRegisterForAnUnknownCustomer() {
        assertThatThrownBy(() -> customerApplicationService.register(request(77, "1000.00", 12, AmortizationMethod.FRENCH)))
                .isInstanceOf(OriginationException.class)
                .extracting(e -> ((OriginationException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listsOnlyTheApplicationsOfTheCustomer() {
        customerApplicationService.register(request(1, "10000.00", 12, AmortizationMethod.FRENCH));

        assertThat(customerApplicationService.findByCustomer(1, null, PageRequest.of(0, 10)))
                .allMatch(application -> application.getCustomerId().equals(1))
                .hasSize(2);
        assertThat(customerApplicationService.findByCustomer(1, LoanApplicationStatus.REGISTERED, PageRequest.of(0, 10)))
                .hasSize(1);
    }

    @Test
    void findsOnlyApprovedApplicationsForTheDisbursement() {
        LoanApplication registered = customerApplicationService.register(request(1, "10000.00", 12, AmortizationMethod.FRENCH));

        assertThat(loanApplicationService.findApproved(9001)).isPresent();
        assertThat(loanApplicationService.findApproved(registered.getId())).isEmpty();
    }

    @Test
    void simulatesALoanWithTheReferenceRateWithoutSavingIt() {
        LoanSimulation simulation = customerApplicationService.simulate(
                new LoanSimulationRequest(new BigDecimal("10000.00"), 12, AmortizationMethod.GERMAN));

        assertThat(simulation.effectiveAnnualRate()).isEqualByComparingTo("0.1550");
        assertThat(simulation.monthlyInsurance()).isEqualByComparingTo("6.00");
        assertThat(simulation.solcaContribution()).isEqualByComparingTo("50.00");
        assertThat(simulation.netAmount()).isEqualByComparingTo("9950.00");
        assertThat(simulation.installments()).hasSize(12);
        assertThat(simulation.firstInstallment()).isGreaterThan(simulation.lastInstallment());
        assertThat(simulation.totalCost()).isEqualByComparingTo(
                simulation.totalInterest().add(new BigDecimal("72.00")).add(new BigDecimal("50.00")));
        assertThat(customerApplicationService.findByCustomer(1, LoanApplicationStatus.REGISTERED, PageRequest.of(0, 10)))
                .isEmpty();
    }

    @Test
    void doesNotSimulateATermOutOfRange() {
        assertThatThrownBy(() -> customerApplicationService.simulate(
                new LoanSimulationRequest(new BigDecimal("10000.00"), 3, AmortizationMethod.FRENCH)))
                .isInstanceOf(OriginationException.class)
                .hasMessageContaining("entre 6 y 60 meses");
    }

    @Test
    void returnsTheCreditLimitsInForce() {
        CreditLimits limits = customerApplicationService.findCreditLimits();

        assertThat(limits.minAmount()).isEqualByComparingTo("500.00");
        assertThat(limits.maxAmount()).isEqualByComparingTo("30000.00");
        assertThat(limits.minTermMonths()).isEqualTo(6);
        assertThat(limits.maxTermMonths()).isEqualTo(60);
    }

    private LoanApplicationRequest request(Integer customerId, String amount, Integer termMonths, AmortizationMethod method) {
        return new LoanApplicationRequest(customerId, new BigDecimal(amount), termMonths, method, CreditPurpose.HEALTH,
                new BigDecimal("3500.00"), new BigDecimal("1200.00"));
    }
}
