
package com.grupob.bancobanquito.originacion.internal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LoanApprovalPolicyTest {

    private final PoliticaAprobacionPrestamo policy =
            new PoliticaAprobacionPrestamo();

    @Test
    void shouldAllowRegisteredApplication() {
        assertDoesNotThrow(() ->
                policy.ensureRegistered("REGISTRADA"));
    }

    @Test
    void shouldRejectAlreadyApprovedApplication() {
        assertThrows(
                ExcepcionReglaDecisionPrestamo.class,
                () -> policy.ensureRegistered("APROBADA")
        );
    }

    @Test
    void shouldRejectAlreadyRejectedApplication() {
        assertThrows(
                ExcepcionReglaDecisionPrestamo.class,
                () -> policy.ensureRegistered("RECHAZADA")
        );
    }

    @Test
    void shouldAllowAnnualRateAtMaximum() {
        assertDoesNotThrow(() ->
                policy.validateAnnualEffectiveRate(
                        new BigDecimal("0.1677"),
                        new BigDecimal("0.1677")
                )
        );
    }

    @Test
    void shouldRejectRateAboveMaximum() {
        assertThrows(
                ExcepcionReglaDecisionPrestamo.class,
                () -> policy.validateAnnualEffectiveRate(
                        new BigDecimal("0.1800"),
                        new BigDecimal("0.1677")
                )
        );
    }

    @Test
    void shouldAllowFrenchInstallment() {
        BigDecimal maximumInstallment =
                policy.validatePaymentCapacity(
                        new BigDecimal("3500.00"),
                        new BigDecimal("1200.00"),
                        new BigDecimal("911.44"),
                        new BigDecimal("0.4000")
                );

        assertEquals(
                new BigDecimal("920.00"),
                maximumInstallment
        );
    }

    @Test
    void shouldRejectGermanInstallment() {
        assertThrows(
                ExcepcionReglaDecisionPrestamo.class,
                () -> policy.validatePaymentCapacity(
                        new BigDecimal("3500.00"),
                        new BigDecimal("1200.00"),
                        new BigDecimal("969.36"),
                        new BigDecimal("0.4000")
                )
        );
    }

    @Test
    void shouldRejectWhenRemainingIncomeIsZero() {
        assertThrows(
                ExcepcionReglaDecisionPrestamo.class,
                () -> policy.validatePaymentCapacity(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("0.4000")
                )
        );
    }
}
