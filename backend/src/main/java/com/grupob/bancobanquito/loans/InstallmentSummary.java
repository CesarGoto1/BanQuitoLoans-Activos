package com.grupob.bancobanquito.loans;
import java.math.BigDecimal;
import java.time.LocalDate;


public record InstallmentSummary(
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal valorCapital,
        BigDecimal valorInteres,
        BigDecimal valorSeguro,
        BigDecimal totalCuota,
        BigDecimal saldoCapitalPosterior,
        BigDecimal capitalPagado,
        BigDecimal interesPagado,
        BigDecimal seguroPagado,
        BigDecimal moraPagada,
        BigDecimal moraAcumulada,
        String estadoCuota,
        LocalDate fechaPago) {
}















