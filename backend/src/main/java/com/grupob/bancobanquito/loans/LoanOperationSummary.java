package com.grupob.bancobanquito.loans;
import java.math.BigDecimal;
import java.time.LocalDate;


public record LoanOperationSummary(
        Integer idOperacion,
        Integer idSolicitud,
        String numeroOperacion,
        String sistemaAmortizacion,
        BigDecimal montoAprobado,
        BigDecimal teaAsignada,
        Integer plazoMeses,
        BigDecimal seguroMensual,
        BigDecimal contribucionSolca,
        BigDecimal montoNeto,
        String cuentaDestino,
        LocalDate fechaDesembolso,
        String estadoInstruccionDesembolso,
        String estadoCartera,
        BigDecimal saldoCapital,
        Integer diasMora) {
}















