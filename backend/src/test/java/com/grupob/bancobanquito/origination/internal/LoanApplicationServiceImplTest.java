package com.grupob.bancobanquito.origination.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.origination.internal.LoanApplication.ApplicationStatus;
import com.grupob.bancobanquito.origination.internal.LoanApplicationServiceImpl.Evaluation;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoanApplicationServiceImplTest {

    private static final Map<String, BigDecimal> PARAMETERS = Map.of(
            "PAR-01", new BigDecimal("500.0000"),
            "PAR-02", new BigDecimal("30000.0000"),
            "PAR-03", new BigDecimal("6.0000"),
            "PAR-04", new BigDecimal("60.0000"),
            "PAR-06", new BigDecimal("0.1550"),
            "PAR-09", new BigDecimal("0.4000"));

    private LoanApplicationRepository loanApplicationRepository;
    private LoanApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        loanApplicationRepository = mock(LoanApplicationRepository.class);
        ParameterService parameterService = mock(ParameterService.class);
        when(parameterService.getValue(anyString())).thenAnswer(i -> PARAMETERS.get(i.getArgument(0, String.class)));
        service = new LoanApplicationServiceImpl(loanApplicationRepository, parameterService,
                mock(BusinessDateService.class));
    }

    @Test
    void frenchInstallmentWithinTheLimitPasses() {
        Evaluation evaluation = service.evaluate(new BigDecimal("3500.00"), new BigDecimal("1200.00"), new BigDecimal("906.21"));

        assertThat(evaluation.passes()).isTrue();
        assertThat(evaluation.monthlySurplus()).isEqualByComparingTo("2300.00");
        assertThat(evaluation.maxAllowedInstallment()).isEqualByComparingTo("920.00");
        assertThat(evaluation.surplusPercentage()).isEqualByComparingTo("39.40");
        assertThat(evaluation.rejectionReason()).isNull();
    }

    @Test
    void firstGermanInstallmentAboveTheLimitFails() {
        Evaluation evaluation = service.evaluate(new BigDecimal("3500.00"), new BigDecimal("1200.00"), new BigDecimal("960.14"));

        assertThat(evaluation.passes()).isFalse();
        assertThat(evaluation.maxAllowedInstallment()).isEqualByComparingTo("920.00");
        assertThat(evaluation.rejectionReason()).contains("960.14", "40 %", "920.00");
    }

    @Test
    void installmentEqualToTheLimitPasses() {
        Evaluation evaluation = service.evaluate(new BigDecimal("3500.00"), new BigDecimal("1200.00"), new BigDecimal("920.00"));

        assertThat(evaluation.passes()).isTrue();
    }

    @Test
    void zeroSurplusFails() {
        Evaluation evaluation = service.evaluate(new BigDecimal("1200.00"), new BigDecimal("1200.00"), new BigDecimal("100.00"));

        assertThat(evaluation.passes()).isFalse();
        assertThat(evaluation.maxAllowedInstallment()).isEqualByComparingTo("0.00");
        assertThat(evaluation.surplusPercentage()).isNull();
        assertThat(evaluation.rejectionReason()).contains("mayor que cero");
    }

    @Test
    void negativeSurplusFails() {
        Evaluation evaluation = service.evaluate(new BigDecimal("1000.00"), new BigDecimal("1200.00"), new BigDecimal("100.00"));

        assertThat(evaluation.passes()).isFalse();
        assertThat(evaluation.monthlySurplus()).isEqualByComparingTo("-200.00");
        assertThat(evaluation.maxAllowedInstallment()).isEqualByComparingTo("0.00");
    }

    @Test
    void amountAndTermOnTheBoundariesAreAccepted() {
        assertThatCode(() -> service.validateRanges(new BigDecimal("500.00"), 6)).doesNotThrowAnyException();
        assertThatCode(() -> service.validateRanges(new BigDecimal("30000.00"), 60)).doesNotThrowAnyException();
    }

    @Test
    void amountOutOfRangeReportsTheAllowedRange() {
        assertThatThrownBy(() -> service.validateRanges(new BigDecimal("499.99"), 12))
                .isInstanceOf(LoanApplicationExceptions.AmountOutOfRange.class)
                .hasMessage("El monto debe estar entre USD 500.00 y USD 30000.00");
        assertThatThrownBy(() -> service.validateRanges(new BigDecimal("30000.01"), 12))
                .isInstanceOf(LoanApplicationExceptions.AmountOutOfRange.class);
    }

    @Test
    void termOutOfRangeReportsTheAllowedRange() {
        assertThatThrownBy(() -> service.validateRanges(new BigDecimal("10000.00"), 5))
                .isInstanceOf(LoanApplicationExceptions.TermOutOfRange.class)
                .hasMessage("El plazo debe estar entre 6 y 60 meses");
        assertThatThrownBy(() -> service.validateRanges(new BigDecimal("10000.00"), 61))
                .isInstanceOf(LoanApplicationExceptions.TermOutOfRange.class);
    }

    @Test
    void customerWithAPendingApplicationCannotRegisterAnother() {
        when(loanApplicationRepository.existsByCustomerIdAndStatus(1, ApplicationStatus.REGISTERED)).thenReturn(true);

        assertThatThrownBy(() -> service.validateNoPendingApplication(1))
                .isInstanceOf(LoanApplicationExceptions.PendingApplication.class);
    }

    @Test
    void customerWithoutAPendingApplicationCanRegister() {
        when(loanApplicationRepository.existsByCustomerIdAndStatus(1, ApplicationStatus.REGISTERED)).thenReturn(false);

        assertThatCode(() -> service.validateNoPendingApplication(1)).doesNotThrowAnyException();
    }
}
